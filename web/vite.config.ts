import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
import { VitePWA } from "vite-plugin-pwa";

// In development the API runs on 8080; proxying keeps the session cookie same-origin.
export default defineConfig({
  plugins: [
    react(),
    VitePWA({
      registerType: "autoUpdate",
      manifest: {
        name: "Express & Possess",
        short_name: "E&P",
        description: "Wish lists for a group: express a wish, let someone take care of it.",
        theme_color: "#0e7c80",
        background_color: "#eef9f8",
        display: "standalone",
        start_url: "/",
        // Rendered from public/icons/android-icon.svg: the letter with room for the circle Android launchers cut out.
        icons: [
          { src: "/icons/icon-192.png", sizes: "192x192", type: "image/png" },
          { src: "/icons/icon-512.png", sizes: "512x512", type: "image/png" },
          { src: "/icons/icon-maskable-512.png", sizes: "512x512", type: "image/png", purpose: "maskable" },
        ],
      },
      workbox: {
        // The API is never cached: every page must show the live state of the group.
        navigateFallbackDenylist: [/^\/api\//],
      },
    }),
  ],
  server: {
    // Listen on the home network too, so a phone on the same Wi-Fi can open the app.
    host: true,
    port: 5173,
    proxy: { "/api": "http://localhost:8080" },
  },
  test: {
    environment: "jsdom",
    globals: false,
    setupFiles: ["./src/test/setup.ts"],
  },
});
