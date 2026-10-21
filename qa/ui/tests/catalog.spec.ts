import {test,expect} from '@playwright/test';
import {CatalogPage} from '../src/pages/CatalogPage';
import {catalogProduct,errorEnvelope,requestId} from '../src/support/contracts';
import {stubApis} from '../src/support/mocks';
const mock=process.env.TEST_MODE!=='integrated';

test.describe('Catálogo por IDs (código Angular)',()=>{
  test.beforeEach(async ({page})=>{if(mock)await stubApis(page);});
  test('FE-020 @smoke consulta ID único via GET',async ({page})=>{
    const catalog=new CatalogPage(page);await catalog.open();
    const request=page.waitForRequest(r=>r.method()==='GET'&&new URL(r.url()).pathname==='/api/catalog/PROD-001');
    await catalog.search('PROD-001');await request;await expect(catalog.rows().first()).toContainText('PROD-001');
  });
  test('FE-021 produto inexistente mostra erro de API',async ({page})=>{
    const catalog=new CatalogPage(page);await catalog.open();await catalog.search('PROD-NAO-EXISTE');
    await expect(page.locator('app-error-state [role="alert"]')).toContainText(requestId);
  });
  test('FE-022 preço ausente usa travessão',async ({page})=>{
    if(mock)await stubApis(page,{product:{status:200,body:{...catalogProduct('PROD-001'),currentPrice:null}}});
    const catalog=new CatalogPage(page);await catalog.open();await catalog.search('PROD-001');
    await expect(catalog.rows().first()).toContainText('—');
  });
  test('FE-023 consulta múltiplos IDs via POST e mostra notFound',async ({page})=>{
    const catalog=new CatalogPage(page);await catalog.open();
    const request=page.waitForRequest(r=>new URL(r.url()).pathname==='/api/catalog/prices'&&r.method()==='POST');
    await catalog.search('PROD-001, PROD-NOT-FOUND');const sent=await request;
    expect(sent.postDataJSON()).toEqual({productIds:['PROD-001','PROD-NOT-FOUND']});
    await expect(page.locator('.alert-warning')).toContainText('PROD-NOT-FOUND');
  });
  test('FE-024 limite de 50 IDs bloqueia envio de 51',async ({page})=>{
    const catalog=new CatalogPage(page);await catalog.open();let count=0;
    page.on('request',r=>{if(new URL(r.url()).pathname==='/api/catalog/prices')count++;});
    await catalog.search(Array.from({length:51},(_,i)=>`PROD-${i+100}`).join(','));
    await expect(page.locator('ngb-toast[role="alert"]')).toBeVisible();expect(count).toBe(0);
  });
  test('FE-025 aviso de lacuna de busca/listagem está visível',async ({page})=>{
    await new CatalogPage(page).open();await expect(page.locator('[role="note"]')).toBeVisible();
  });
  test('CAT-UX-01 normaliza duplicatas e separadores no lote',async ({page})=>{
    const catalog=new CatalogPage(page);await catalog.open();
    const req=page.waitForRequest(r=>r.method()==='POST'&&new URL(r.url()).pathname==='/api/catalog/prices');
    await catalog.search('PROD-001; PROD-002, PROD-001');expect((await req).postDataJSON()).toEqual({productIds:['PROD-001','PROD-002']});
  });
});
