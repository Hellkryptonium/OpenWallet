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
    '/getOwnersForNFT?contractAddress=' +
    encodeURIComponent(contractAddress) +
    '&tokenId=' +
    encodeURIComponent(tokenId);

  const { status, json } = await getJson(url);
  console.log('HTTP', status, 'contract', contractAddress, 'tokenId', tokenId);
  if (!json) {
    console.log('non-JSON response');
    return;
  }
  const owners = Array.isArray(json.owners) ? json.owners : [];
  console.log('ownersCount', owners.length);
  console.log('firstOwner', owners[0] ?? null);
})();
