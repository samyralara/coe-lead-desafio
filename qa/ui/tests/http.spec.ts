import {test,expect} from '@playwright/test';
import {stubApis} from '../src/support/mocks';
import {client,errorEnvelope,requestId} from '../src/support/contracts';
import {OrderFormPage} from '../src/pages/OrderFormPage';
const mock=process.env.TEST_MODE!=='integrated';
test.describe('HTTP, interceptadores e rotas',()=>{
  test.beforeEach(async ({page})=>{if(mock)await stubApis(page);});
  test('HTTP-01 @smoke cada request API leva X-Request-Id UUID',async ({page})=>{
    const req=page.waitForRequest(r=>new URL(r.url()).pathname==='/api/orders'&&r.method()==='GET');
    await page.goto('/orders');const header=(await req).headers()['x-request-id'];
    expect(header).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i);
  });
  test('HTTP-02 erro com envelope exibe requestId no UI',async ({page})=>{
    if(mock)await stubApis(page,{list:{status:503,body:errorEnvelope('CATALOG_UNAVAILABLE')}});
    await page.goto('/orders');await expect(page.locator('app-error-state [role="alert"]')).toContainText(requestId);
  });
  test('HTTP-03 falha de rede gera estado de erro',async ({page})=>{
    await page.route(/\/api\/orders(?:\?|$)/,route=>route.abort('failed'));
    await page.goto('/orders');await expect(page.locator('app-error-state [role="alert"]')).toBeVisible();
  });
  test('HTTP-04 resposta HTML 502 apresenta fallback genérico',async ({page})=>{
    await page.route(/\/api\/orders(?:\?|$)/,route=>route.fulfill({status:502,contentType:'text/html',body:'<h1>bad gateway</h1>'}));
    await page.goto('/orders');await expect(page.locator('app-error-state [role="alert"]')).toBeVisible();
  });
  test('HTTP-05 URLs relativas de ambiente apontam para /api/orders',async ({page})=>{
    const req=page.waitForRequest(r=>r.method()==='GET'&&new URL(r.url()).pathname==='/api/orders');
    await page.goto('/orders');const request=await req;expect(new URL(request.url()).origin).toBe(new URL(page.url()).origin);
  });
  test('HTTP-06 criação envia POST relativo, com JSON e cabeçalho de rastreio',async ({page})=>{
    const form=new OrderFormPage(page);await form.open();await form.fillClient(client);await form.addItem('PROD-001');
    const pending=page.waitForRequest(r=>r.method()==='POST'&&new URL(r.url()).pathname==='/api/orders');
    await form.submit();const request=await pending;expect(request.headers()['x-request-id']).toBeTruthy();expect(request.postDataJSON().items).toEqual([{productId:'PROD-001',quantity:1}]);
  });
});
