# Kuky Frontend

Frontend application for the Kuky project.

## Getting Started

### Installation

Install dependencies using npm:

```bash
npm install
```

### Running the Application

Start the development server:

```bash
npm start
```

## Project Structure

- `src/` - Source files
- `package.json` - Project dependencies and scripts

## Production deployment

`npm run build` targets Node (nitro's `node-server` preset — see `vite.config.ts`) and
emits a self-contained server at `.output/server/index.mjs`. Set `VITE_API_BASE_URL` and
`VITE_SITE_URL` (see `.env.example`) at build time, since they're baked into the client
bundle.

```bash
npm run build
npm start          # node .output/server/index.mjs — listens on $PORT (default 3000), $HOST
```

Or build/run the provided `Dockerfile`, which does the same in a multi-stage image
(listens on port 8080):

```bash
docker build \
  --build-arg VITE_API_BASE_URL=https://api.kuky.es \
  --build-arg VITE_SITE_URL=https://kuky.es \
  -t kuky-frontend .
docker run -p 8080:8080 kuky-frontend
```
