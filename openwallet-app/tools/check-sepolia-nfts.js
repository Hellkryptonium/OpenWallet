const fs = require('fs');
const https = require('https');

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

(async () => {
  const owner = process.argv[2] || '0xbB619acf5dA91a148633927f63340924ADD8F217';
  const withMetadata = (process.argv[3] || 'true').toLowerCase() !== 'false';
  const contractAddress = process.argv[4] || '';
  const key = process.env.OPENWALLET_ALCHEMY_API_KEY || process.env.ALCHEMY_API_KEY || readAlchemyKeyFromDbProperties();
  if (!key) {
    console.error('Missing Alchemy key. Set OPENWALLET_ALCHEMY_API_KEY or ALCHEMY_API_KEY, or set rpc.url in db.properties to an Alchemy /v2/<key> URL.');
    process.exit(1);
  }

  let url =
    'https://eth-sepolia.g.alchemy.com/nft/v3/' +
    key +
    '/getNFTsForOwner?owner=' +
    encodeURIComponent(owner) +
    '&withMetadata=' +
    encodeURIComponent(String(withMetadata)) +
    '&pageSize=100';

  if (contractAddress && contractAddress.trim().length) {
    url += '&contractAddresses[]=' + encodeURIComponent(contractAddress.trim());
  }

  const { status, json } = await getJson(url);
  const keys = json ? Object.keys(json) : [];
  const count = json && Array.isArray(json.ownedNfts) ? json.ownedNfts.length : 0;
  console.log('HTTP', status, 'ownedNfts', count, 'owner', owner, 'withMetadata', withMetadata, 'contract', contractAddress || null);
  console.log('topLevelKeys', keys);
  if (json && (json.error || json.message)) {
    console.log('error', json.error ?? null);
    console.log('message', json.message ?? null);
  }
  if (count > 0) {
    const first = json.ownedNfts[0];
    console.log('first.contract', first?.contract?.address ?? null);
    console.log('first.tokenId', first?.tokenId ?? null);
  }
})();
