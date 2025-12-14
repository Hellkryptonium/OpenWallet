#!/usr/bin/env node
// Usage:
//   node tools/check-erc721-transferlogs.js <contract> <toAddress> [fromBlock] [toBlock]
// Reads rpc.url from src/main/resources/db.properties or env OPENWALLET_RPC_URL.

const fs = require('fs');

function readRpcUrl() {
  const env = process.env.OPENWALLET_RPC_URL;
  if (env && env.trim()) return env.trim();

  const p = fs.readFileSync('src/main/resources/db.properties', 'utf8');
  const line = p.split('\n').find(l => l.trim().startsWith('rpc.url'));
  if (!line) return '';
  return line.split('=').slice(1).join('=').trim();
}

function topicAddress(addr) {
  let a = (addr || '').trim().toLowerCase();
  if (a.startsWith('0x')) a = a.slice(2);
  if (a.length !== 40) throw new Error('bad address: ' + addr);
  return '0x' + '0'.repeat(24) + a;
}

(async () => {
  const [contract, toAddress, fromBlockArg, toBlockArg] = process.argv.slice(2);
  if (!contract || !toAddress) {
    console.error('Usage: node tools/check-erc721-transferlogs.js <contract> <toAddress> [fromBlock] [toBlock]');
    process.exit(1);
  }

  const rpcUrl = readRpcUrl();
  if (!rpcUrl) throw new Error('Missing rpc.url in db.properties (or set OPENWALLET_RPC_URL).');

  const transferTopic = '0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef';

  const payload = {
    jsonrpc: '2.0',
    id: 1,
    method: 'eth_getLogs',
    params: [
      {
        address: contract,
        fromBlock: fromBlockArg || '0x0',
        toBlock: toBlockArg || 'latest',
        topics: [transferTopic, null, topicAddress(toAddress)]
      }
    ]
  };

  const res = await fetch(rpcUrl, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(payload)
  });

  const json = await res.json();
  if (json.error) {
    console.error('RPC error:', json.error);
    process.exit(2);
  }

  const logs = json.result || [];
  console.log('logs', logs.length);
  for (const l of logs.slice(0, 10)) {
    const tokenIdTopic = l.topics && l.topics[3];
    const tokenId = tokenIdTopic ? BigInt(tokenIdTopic).toString() : '?';
    console.log('-', 'block', parseInt(l.blockNumber, 16), 'tokenId', tokenId, 'tx', l.transactionHash);
  }
})();
