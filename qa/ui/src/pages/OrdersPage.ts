import { type Page, expect } from '@playwright/test';
export class OrdersPage {
  constructor(private readonly page: Page) {}
  async open(): Promise<void> { await this.page.goto('/orders'); await expect(this.page.locator('h1')).toBeVisible(); }
  rows() { return this.page.locator('table tbody tr'); }
  async openFirstDetail(): Promise<void> { await this.rows().first().locator('a[href^="/orders/"]').click(); }
  pagination() { return this.page.locator('ngb-pagination'); }
}
