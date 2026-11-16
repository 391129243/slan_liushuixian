import { defineConfig } from '@playwright/test'
export default defineConfig({
  testDir: './tests', workers: 1, timeout: 30000,
  use: { baseURL: 'http://127.0.0.1:5174', viewport: { width: 1440, height: 1100 },
    launchOptions: { executablePath: process.env.PLAYWRIGHT_CHROME || 'C:/Program Files/Google/Chrome/Application/chrome.exe' } },
  webServer: { command: 'npm run dev -- --port 5174 --strictPort', url: 'http://127.0.0.1:5174', reuseExistingServer: false },
  reporter: 'list'
})
