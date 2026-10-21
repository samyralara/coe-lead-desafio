import {test,expect} from '@playwright/test';
import {stubApis} from '../src/support/mocks';
import {pagedOrders} from '../src/support/contracts';
const mock=process.env.TEST_MODE!=='integrated';
test.describe('Dashboard',()=>{
  test('DASH-01 indicador calculado sobre página carregada e aviso de gap',async ({page})=>{
    if(mock)await stubApis(page,{list:{status:200,body:{...pagedOrders(0),totalElements:120,totalPages:2}}});
    await page.goto('/');await expect(page.locator('[role="note"]')).toBeVisible();
    await expect(page.getByText('6.749,91',{exact:false})).toBeVisible();
  });
  test('DASH-02 estado vazio preserva aviso',async ({page})=>{
    if(mock)await stubApis(page,{list:{status:200,body:pagedOrders(0,[])}});
    await page.goto('/');await expect(page.locator('app-empty-state')).toBeVisible();await expect(page.locator('[role="note"]')).toBeVisible();
  });
});
