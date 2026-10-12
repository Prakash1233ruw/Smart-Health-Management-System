import { defineConfig } from "vite";
const proxy = {
  "/api": {
    target: process.env.BACKEND_URL || "http://localhost:8080",
    changeOrigin: true,
  },
  "/auth": {
    target: process.env.BACKEND_URL || "http://localhost:8080",
    changeOrigin: true,
  },
};
export default defineConfig({
  server: { port: 5173, strictPort: true, host: "127.0.0.1", proxy },
  preview: { host: "127.0.0.1", proxy },
});
// Local font dependencies are bundled with the app.
