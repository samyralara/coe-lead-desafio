import { type Page, expect } from '@playwright/test';
export class CatalogPage {
  constructor(private readonly page: Page) {}
  async open(): Promise<void> { await this.page.goto('/catalog'); await expect(this.page.locator('#catalog-ids')).toBeVisible(); }
  async search(ids: string): Promise<void> { await this.page.locator('#catalog-ids').fill(ids); await this.page.locator('form button[type="submit"]').click(); }
  rows() { return this.page.locator('table tbody tr'); }
}
