const fs = require('fs');
const https = require('https');
const http = require('http');
const path = require('path');

function readAlchemyKeyFromDbProperties() {
  const props = fs.readFileSync('src/main/resources/db.properties', 'utf8');
  const rpcLine = props.split(/\r?\n/).find((l) => l.trim().startsWith('rpc.url=')) || '';
  const rpc = (rpcLine.split('=')[1] || '').trim();
  const m = rpc.match(/\/v2\/([^?\s]+)/);
  return m ? m[1] : '';
}

function getJson(url) {
  return new Promise((resolve, reject) => {
    https
      .get(url, (res) => {
        let data = '';
        res.on('data', (c) => (data += c));
        res.on('end', () => {
          try {
            resolve({ status: res.statusCode, json: JSON.parse(data) });
          } catch (e) {
            resolve({ status: res.statusCode, json: null });
          }
        });
      })
      .on('error', reject);
  });
}

function normalizeIpfs(url) {
  if (!url) return '';
  const s = String(url).trim();
  if (!s) return '';
  if (s.startsWith('ipfs://')) {
    return 'https://ipfs.io/ipfs/' + s.replace('ipfs://', '').replace(/^ipfs\//, '');
  }
  return s;
}

function pickImageUrl(json) {
  const candidates = [
    json?.image?.cachedUrl,
    json?.image?.pngUrl,
    json?.image?.thumbnailUrl,
    json?.image?.originalUrl,
    json?.media && Array.isArray(json.media) ? json.media[0]?.gateway : null,
    json?.metadata?.image,
    json?.raw?.metadata?.image,
    json?.raw?.metadata?.image_url,
    json?.metadata?.image_url,
  ].filter(Boolean);

  for (const c of candidates) {
    const u = normalizeIpfs(c);
    if (u && (u.startsWith('http://') || u.startsWith('https://'))) return u;
  }
  return '';
}

function downloadToFile(url, outFile, maxRedirects = 5) {
  return new Promise((resolve, reject) => {
    const u = new URL(url);
    const mod = u.protocol === 'http:' ? http : https;

    const req = mod.request(
      {
        protocol: u.protocol,
        hostname: u.hostname,
        port: u.port || undefined,
        path: u.pathname + (u.search || ''),
        method: 'GET',
        headers: {
          'User-Agent': 'OpenWallet-Tools/1.0',
          Accept: 'image/*,*/*;q=0.8',
        },
      },
      (res) => {
        const code = res.statusCode || 0;

        if ([301, 302, 303, 307, 308].includes(code) && res.headers.location && maxRedirects > 0) {
          const next = new URL(res.headers.location, url).toString();
          res.resume();
          downloadToFile(next, outFile, maxRedirects - 1).then(resolve, reject);
          return;
        }

        if (code < 200 || code >= 300) {
          let body = '';
          res.on('data', (c) => (body += c));
          res.on('end', () => reject(new Error('HTTP ' + code + ' downloading image. Body: ' + body.slice(0, 300))));
          return;
        }

        fs.mkdirSync(path.dirname(outFile), { recursive: true });
        const file = fs.createWriteStream(outFile);
        res.pipe(file);
        file.on('finish', () => file.close(() => resolve({ outFile, contentType: res.headers['content-type'] || '' })));
        file.on('error', reject);
      }
    );
    req.on('error', reject);
    req.end();
  });
}

(async () => {
  const contractAddress = process.argv[2] || '0x95E98b9082bb5c7A37692BdFBfcFE7626b40a844';
  const tokenId = process.argv[3] || '0';

  const key = process.env.OPENWALLET_ALCHEMY_API_KEY || process.env.ALCHEMY_API_KEY || readAlchemyKeyFromDbProperties();
  if (!key) {
    console.error('Missing Alchemy key.');
    process.exit(1);
  }

  const url =
    'https://eth-sepolia.g.alchemy.com/nft/v3/' +
    key +
    '/getNFTMetadata?contractAddress=' +
    encodeURIComponent(contractAddress) +
    '&tokenId=' +
    encodeURIComponent(tokenId) +
    '&tokenType=ERC721';

  const { status, json } = await getJson(url);
  console.log('HTTP', status, 'contract', contractAddress, 'tokenId', tokenId);
  if (!json) {
    console.log('non-JSON response');
    return;
  }
  console.log('name', json.name ?? null);
  console.log('title', json.title ?? null);
  console.log('tokenUri', json.tokenUri?.raw ?? json.tokenUri ?? null);

  const imageUrl = pickImageUrl(json);
  console.log('imageUrl', imageUrl || null);
  if (!imageUrl) {
    console.log('No image URL found in response.');
    return;
  }

  const safeContract = contractAddress.toLowerCase();
  const outFile = path.join('tools', 'out', `${safeContract}_${tokenId}.img`);
  const { outFile: saved, contentType } = await downloadToFile(imageUrl, outFile);
  console.log('downloaded', saved, 'content-type', contentType || null);
})();
