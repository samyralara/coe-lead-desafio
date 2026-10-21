import { type Page, expect } from '@playwright/test';
import type { Client } from '../support/contracts';

/** Locators sourced from order-form.page.html: no fabricated data-testid selectors. */
export class OrderFormPage {
  constructor(private readonly page: Page) {}
  async open(): Promise<void> { await this.page.goto('/orders/new'); await expect(this.page.locator('#client-name')).toBeVisible(); }
  async fillClient(client: Partial<Client>): Promise<void> {
    for (const [key, value] of Object.entries(client)) {
      const selector = key === 'name' ? '#client-name' : key === 'document' ? '#client-document' : '#client-email';
      await this.page.locator(selector).fill(value ?? '');
    }
  }
  async addItem(productId: string, quantity = 1): Promise<void> {
    await this.page.locator('#item-product-id').fill(productId);
    await this.page.locator('#item-quantity').fill(String(quantity));
    await this.page.locator('form button[type="submit"]').click();
  }
  async submit(): Promise<void> { await this.page.locator('.card').last().locator('button.btn-primary').click(); }
  itemRows() { return this.page.locator('form table tbody tr'); }
  toast() { return this.page.locator('ngb-toast[role="alert"]'); }
  async expectToast(): Promise<void> { await expect(this.toast().first()).toBeVisible(); }
}
