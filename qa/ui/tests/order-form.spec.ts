import { test, expect } from '@playwright/test';
import { OrderFormPage } from '../src/pages/OrderFormPage';
import { client, order, errorEnvelope, requestId } from '../src/support/contracts';
import { stubApis } from '../src/support/mocks';

const mock = process.env.TEST_MODE !== 'integrated';
test.describe('Formulário real Angular', () => {
  test.beforeEach(async ({page}) => { if (mock) await stubApis(page); });
  test('FE-001 @smoke criar pedido, validar payload e navegar para detalhes', async ({page}) => {
    const form = new OrderFormPage(page); await form.open(); await form.fillClient(client);
    await form.addItem('PROD-001',2); await expect(form.itemRows()).toHaveCount(1);
    await form.addItem('PROD-002',1); await expect(form.itemRows()).toHaveCount(2);
    const postPromise = page.waitForRequest(r=>new URL(r.url()).pathname==='/api/orders'&&r.method()==='POST');
    await form.submit(); const post = await postPromise;
    expect(post.postDataJSON()).toEqual({client,items:[{productId:'PROD-001',quantity:2},{productId:'PROD-002',quantity:1}]});
    await expect(page).toHaveURL(new RegExp(`/orders/${order.id}$`));
    await expect(page.getByText('6,749.91', {exact:false})).toBeVisible();
  });
  test('FE-002 nome obrigatório impede POST', async ({page}) => {
    const form=new OrderFormPage(page); await form.open(); await form.fillClient({...client,name:''}); await form.addItem('PROD-001');
    let posts=0; page.on('request',r=>{if(r.method()==='POST'&&new URL(r.url()).pathname==='/api/orders')posts++;});
    await form.submit(); await expect(page.locator('#client-name')).toHaveClass(/is-invalid/); expect(posts).toBe(0);
  });
  test('FE-003 email inválido impede POST',async ({page})=>{
    const form=new OrderFormPage(page); await form.open(); await form.fillClient({...client,email:'invalid'}); await form.addItem('PROD-001');
    await form.submit(); await expect(page.locator('#client-email')).toHaveClass(/is-invalid/);
  });
  for(const quantity of [0,1000]) test(`FE-004 AVL quantidade ${quantity} bloqueada`,async ({page})=>{
    const form=new OrderFormPage(page); await form.open(); await form.addItem('PROD-001',quantity);
    await expect(page.locator('#item-quantity')).toHaveClass(/is-invalid/); await expect(form.itemRows()).toHaveCount(0);
  });
  test('FE-005 rejeitar duplicidade e manter um único item',async ({page})=>{
    const form=new OrderFormPage(page); await form.open(); await form.addItem('PROD-001'); await form.addItem('PROD-001');
    await expect(form.itemRows()).toHaveCount(1); await form.expectToast();
  });
  for(const [id, productId, data] of [
    ['FE-006','PROD-INATIVO',{productId:'PROD-INATIVO',name:'Inativo',status:'INATIVO',currentPrice:{value:100,currency:'BRL',validFrom:'2026-01-01T00:00:00Z',validTo:null}}],
    ['FE-007','PROD-SEM-PRECO',{productId:'PROD-SEM-PRECO',name:'Sem preço',status:'ATIVO',currentPrice:null}],
  ] as const) test(`${id} recusa produto na validação prévia do catálogo`, async ({page})=>{
    if(mock) await stubApis(page,{batch:{status:200,body:{prices:[data],notFound:[]}}});
    const form=new OrderFormPage(page); await form.open(); await form.addItem(productId);
    await form.expectToast(); await expect(form.itemRows()).toHaveCount(0);
  });
  test('FE-008 subtotal é informativo, total e desconto vêm do detalhe',async ({page})=>{
    const form=new OrderFormPage(page); await form.open(); await form.fillClient(client); await form.addItem('PROD-001',2);
    await expect(page.getByText('7.000,00',{exact:false}).first()).toBeVisible();
    await form.submit(); await expect(page).toHaveURL(/\/orders\/[^/]+$/);
    await expect(page.getByText('749,99',{exact:false})).toBeVisible();
  });
  test('FE-009 falha de rede na criação exibe toast',async ({page})=>{
    await page.route(/\/api\/orders$/,r=>r.request().method()==='POST'?r.abort('failed'):r.fallback());
    const form=new OrderFormPage(page); await form.open();await form.fillClient(client); await form.addItem('PROD-001');await form.submit();await form.expectToast();
  });
  test('FE-010 botão fica desabilitado durante POST pendente',async ({page})=>{
    let finish!:()=>void; const gate=new Promise<void>(resolve=>finish=resolve);
    await page.route(/\/api\/orders$/,async route=>{
      if(route.request().method()!=='POST')return route.fallback(); await gate; await route.fulfill({status:201,contentType:'application/json',body:JSON.stringify(order)});
    });
    const form=new OrderFormPage(page);await form.open();await form.fillClient(client);await form.addItem('PROD-001');
    await form.submit(); const button=page.locator('.card').last().locator('button.btn-primary');
    try{await expect(button).toBeDisabled();}finally{finish();}
    await expect(page).toHaveURL(/\/orders\/[^/]+$/);
  });
  test('FE-011 erro 400 exibe requestId no toast',async ({page})=>{
    if(mock) await stubApis(page,{create:{status:400,body:errorEnvelope('VALIDATION_ERROR',['items'])}});
    const form=new OrderFormPage(page);await form.open();await form.fillClient(client);await form.addItem('PROD-001');await form.submit();
    await expect(form.toast()).toContainText(requestId);
  });
});
