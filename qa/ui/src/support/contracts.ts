export type Item = { productId: string; quantity: number };
export type Client = { name: string; document: string; email: string };
export type OrderInput = { client: Client; items: Item[] };
export const client: Client = { name: 'Maria Silva', document: '12345678901', email: 'maria@example.com' };
export const products = [
  { id: 'PROD-001', name: 'Notebook Dell Inspiron 15', price: 3500 },
  { id: 'PROD-002', name: 'Mouse Logitech MX Master 3S', price: 499.90 },
] as const;
export const requestId = '3f2c1b8e-2f2a-4c1e-9a1b-111111111111';
export const order = {
  id: '550e8400-e29b-41d4-a716-446655440000', client,
  items: [
    { productId: products[0].id, productName: products[0].name, unitPrice: 3500, quantity: 2, lineTotal: 7000 },
    { productId: products[1].id, productName: products[1].name, unitPrice: 499.90, quantity: 1, lineTotal: 499.90 },
  ], subtotal: 7499.90, discount: 749.99, total: 6749.91,
  status: 'CRIADO', createdAt: '2026-09-25T17:00:00Z',
};
export const errorEnvelope = (code: string, details: string[] = []) => ({
  error: { code, message: code, details }, requestId,
});
export const pagedOrders = (page = 0, orders = [order]) => ({
  content: orders, page, size: 20, totalElements: orders.length, totalPages: Math.ceil(orders.length / 20),
});
export const catalogProduct = (id: string) => {
  const product = products.find((p) => p.id === id);
  return product ? { id: product.id, name: product.name, description: 'Produto de teste', status: 'ATIVO', currentPrice: { value: product.price, currency: 'BRL', validFrom: '2026-01-01T00:00:00Z', validTo: null } } : null;
};
