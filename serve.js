const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 8888;
const HTML_FILE = './chat-test.html';

const server = http.createServer((req, res) => {
  if (req.url === '/chat-test.html' || req.url === '/') {
    res.writeHead(200, { 'Content-Type': 'text/html' });
    res.end(fs.readFileSync(HTML_FILE));
  } else {
    res.writeHead(404);
    res.end('Not Found');
  }
});

server.listen(PORT, () => {
  console.log(`Chat test server running at http://localhost:${PORT}/chat-test.html`);
});
