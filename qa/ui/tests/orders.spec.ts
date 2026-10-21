import {test,expect} from '@playwright/test';
import {OrdersPage} from '../src/pages/OrdersPage';
import {order,pagedOrders,errorEnvelope,requestId} from '../src/support/contracts';
import {stubApis} from '../src/support/mocks';
const mock=process.env.TEST_MODE!=='integrated';
test.describe('Listagem e detalhes',()=>{
  test.beforeEach(async ({page})=>{if(mock)await stubApis(page);});
  test('FE-012 @smoke lista pedido com cliente e total',async ({page})=>{
    const orders=new OrdersPage(page);await orders.open();await expect(orders.rows()).toHaveCount(1);
    await expect(orders.rows().first()).toContainText('Maria Silva');await expect(orders.rows().first()).toContainText('6,749.91');
  });
  test('FE-013 estado vazio',async ({page})=>{
    if(mock)await stubApis(page,{list:{status:200,body:pagedOrders(0,[])}});
    const orders=new OrdersPage(page);await orders.open();await expect(orders.rows()).toHaveCount(0);await expect(page.locator('app-empty-state')).toBeVisible();
  });
  test('FE-014 converte navegação de página 2 para page=1',async ({page})=>{
    if(mock)await stubApis(page,{list:{status:200,body:{...pagedOrders(0),totalElements:21,totalPages:2}}});
    const orders=new OrdersPage(page);await orders.open();await expect(orders.pagination()).toBeVisible();
    const request=page.waitForRequest(r=>r.url().includes('/api/orders')&&new URL(r.url()).searchParams.get('page')==='1');
    await orders.pagination().getByText('2',{exact:true}).click();await request;
  });
  test('FE-016 detalhe por rota com dados da API',async ({page})=>{
    await page.goto(`/orders/${order.id}`);await expect(page.getByText(order.client.email)).toBeVisible();
    await expect(page.getByText(order.id,{exact:true})).toBeVisible();
  });
  test('FE-017 pedido inexistente exibe estado de erro',async ({page})=>{
    if(mock)await stubApis(page,{detail:{status:404,body:errorEnvelope('ORDER_NOT_FOUND')}});
    await page.goto('/orders/00000000-0000-0000-0000-000000000000');
    await expect(page.locator('app-error-state [role="alert"]')).toContainText(requestId);
  });
  test('FE-018 BRL e status CRIADO na listagem',async ({page})=>{
    await new OrdersPage(page).open();const row=page.locator('table tbody tr').first();
    await expect(row).toContainText('6.749,91');await expect(row).toContainText('CRIADO');
  });
  test('FE-019 erro HTTP na listagem e ação de tentar novamente',async ({page})=>{
    if(mock)await stubApis(page,{list:{status:503,body:errorEnvelope('CATALOG_UNAVAILABLE')}});
    await new OrdersPage(page).open();await expect(page.locator('app-error-state [role="alert"]')).toContainText(requestId);
    await expect(page.locator('app-error-state button')).toBeVisible();
  });
});
