import type { Page, Route } from '@playwright/test';
import { catalogProduct, errorEnvelope, order, pagedOrders, products, requestId } from './contracts';

export interface MockReply { status: number; body: unknown }
export interface MockOverrides { list?: MockReply; create?: MockReply; product?: MockReply; batch?: MockReply; detail?: MockReply }

function respond(route: Route, reply: MockReply): Promise<void> {
  return route.fulfill({status: reply.status, contentType:'application/json', headers:{'X-Request-Id':requestId}, body:JSON.stringify(reply.body)});
}
/** Stubs only HTTP API endpoints; Angular routing, forms and interceptors stay real. */
export async function stubApis(page: Page, overrides: MockOverrides = {}): Promise<void> {
  await page.route(/\/api\/orders(?:\/|\?|$)/, async route => {
    const request = route.request();
    const path = new URL(request.url()).pathname;
    if (request.method() === 'POST') return respond(route, overrides.create ?? {status:201,body:order});
    if (request.method() === 'GET' && path !== '/api/orders') return respond(route, overrides.detail ?? {status:200,body:order});
    if (request.method() === 'GET') {
      const params = new URL(request.url()).searchParams;
      return respond(route, overrides.list ?? {status:200,body:pagedOrders(Number(params.get('page') ?? 0))});
    }
    return route.fallback();
  });
  await page.route(/\/api\/catalog(?:\/|\?|$)/, async route => {
    const request = route.request();
    if (request.method() === 'POST') {
      if (overrides.batch) return respond(route, overrides.batch);
      const ids = (request.postDataJSON() as {productIds:string[]}).productIds;
      const prices = ids.map(id => catalogProduct(id)).filter((value): value is NonNullable<typeof value> => value != null).map(p => ({productId:p.id,name:p.name,status:p.status,currentPrice:p.currentPrice}));
      return respond(route, {status:200,body:{prices,notFound:ids.filter(id=>!products.some(p=>p.id===id))}});
    }
    const id = decodeURIComponent(new URL(request.url()).pathname.split('/').pop() ?? '');
    const product = catalogProduct(id);
    return respond(route, overrides.product ?? (product ? {status:200,body:product}:{status:404,body:errorEnvelope('PRODUCT_NOT_FOUND',[id])}));
  });
}
