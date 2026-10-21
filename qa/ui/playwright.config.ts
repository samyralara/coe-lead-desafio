import { defineConfig, devices } from '@playwright/test';
import 'dotenv/config';

const baseURL = process.env.BASE_URL ?? 'http://127.0.0.1:4200';
export default defineConfig({
  testDir: './tests', timeout: 45_000,
  expect: {timeout: 10_000}, fullyParallel: true,
  forbidOnly: Boolean(process.env.CI), retries: process.env.CI ? 1 : 0,
  workers: process.env.CI ? 2 : undefined,
  reporter: [['list'], ['html', {open:'never'}], ['junit',{outputFile:'test-results/junit.xml'}]],
  use: {baseURL, trace:'retain-on-failure', screenshot:'only-on-failure', video:'retain-on-failure'},
  projects: [{name:'chromium',use:{...devices['Desktop Chrome']}}],
  webServer: process.env.START_APP === '1' ? {
    command: 'npm --prefix ./application/frontend start -- --host 127.0.0.1',
    url: baseURL, reuseExistingServer: !process.env.CI, timeout: 120_000,
  } : undefined,
});
