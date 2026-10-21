import {test,expect} from '@playwright/test';
import {OrderFormPage} from '../src/pages/OrderFormPage';
import {client} from '../src/support/contracts';
/** Run only with TEST_MODE=integrated and live catalog/order-service. No API stubbing. */

test('E2E-001 @integrated cria pedido e consulta detalhe nos serviços reais',async ({page})=>{
  
  test.skip(process.env.TEST_MODE!=='integrated','Requires running PostgreSQL, MongoDB and APIs with seeded PROD-001');
  const form=new OrderFormPage(page);await form.open();await form.fillClient(client);await form.addItem('PROD-001',1);
  await expect(form.itemRows()).toHaveCount(1);
  await form.submit();await expect(page).toHaveURL(/\/orders\/[0-9a-f-]{36}$/i);
  await expect(page.getByText(client.email)).toBeVisible();
});
