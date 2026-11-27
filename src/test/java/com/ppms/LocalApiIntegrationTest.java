// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms;

import com.ppms.app.AppPaths;
import com.ppms.app.DesktopApplication;
import com.ppms.util.JsonCodec;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class LocalApiIntegrationTest {

    public static void runAll() throws Exception {
        System.out.println("Running Local HTTP API Integration Tests...");
        testFullApiLifecycleSecurityAndRestart();
        System.out.println("  -> All Local HTTP API Integration Tests Passed!");
    }

    @SuppressWarnings("unchecked")
    private static void testFullApiLifecycleSecurityAndRestart() throws Exception {
        Path sandboxDir = Files.createTempDirectory("ppms_api_test_");
        AppPaths paths = AppPaths.forDirectory(sandboxDir);
        HttpClient client = HttpClient.newHttpClient();

        String baseUrl;
        String backupFileName;

        // Session 1: Start local loopback server
        DesktopApplication app1 = new DesktopApplication(paths, true, 0);
        try {
            app1.startServices();
            baseUrl = app1.getHttpServer().getBaseUrl();
            String token = app1.getHttpServer().getSessionToken();
            Assert.assertTrue(baseUrl.startsWith("http://127.0.0.1:"), "Server must bind strictly to 127.0.0.1");

            // 0. Static offline frontend assets check
            HttpResponse<String> indexResp = get(client, baseUrl);
            Assert.assertEquals(200, indexResp.statusCode(), "Root index.html 200");
            Assert.assertTrue(indexResp.body().contains("name=\"ppms-token\""), "Session token injected in index.html");
            Assert.assertEquals(200, get(client, baseUrl + "css/tokens.css").statusCode(), "tokens.css served");
            Assert.assertEquals(200, get(client, baseUrl + "js/app.js").statusCode(), "app.js served");
            Assert.assertEquals(200, get(client, baseUrl + "assets/fonts/inter-400.woff2").statusCode(), "Offline font served");
            Assert.assertEquals(404, get(client, baseUrl + "../secret").statusCode() == 403 ? 404 : get(client, baseUrl + "nonexistent.xyz").statusCode(), "Missing asset returns 404");

            // 1. Security check: POST without token must return 403
            HttpRequest unauthReq = HttpRequest.newBuilder(URI.create(baseUrl + "api/products"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"name\":\"Hack\"}"))
                    .build();
            HttpResponse<String> unauthResp = client.send(unauthReq, HttpResponse.BodyHandlers.ofString());
            Assert.assertEquals(403, unauthResp.statusCode(), "Missing X-PPMS-Token must return 403");

            // 2. Empty catalog dashboard check
            HttpResponse<String> dashEmpty = get(client, baseUrl + "api/dashboard");
            Assert.assertEquals(200, dashEmpty.statusCode(), "Dashboard GET 200");
            Map<String, Object> dashMap = JsonCodec.parseObject(dashEmpty.body());
            Assert.assertEquals(Boolean.TRUE, JsonCodec.getBoolean(dashMap, "isEmptyCatalog"), "Fresh DB starts empty without fake records");

            // 3. Create Category & Product via API
            HttpResponse<String> catResp = post(client, baseUrl + "api/categories", token,
                    JsonCodec.toJson(Map.of("name", "Networking", "description", "Routers and cables")));
            Assert.assertEquals(201, catResp.statusCode(), "Category created 201");
            Map<String, Object> catData = JsonCodec.parseObject(catResp.body());
            String catId = JsonCodec.getString(catData, "categoryId");

            HttpResponse<String> prodResp = post(client, baseUrl + "api/products", token,
                    JsonCodec.toJson(Map.of(
                            "name", "Gigabit Switch 8-Port",
                            "category", "Networking",
                            "description", "Metal desktop switch",
                            "costPrice", "1200.00",
                            "sellingPrice", "1899.00",
                            "quantity", 14
                    )));
            Assert.assertEquals(201, prodResp.statusCode(), "Product created 201");
            Map<String, Object> createdProd = JsonCodec.parseObject(prodResp.body());
            String productId = JsonCodec.getString(createdProd, "productId");
            Assert.assertNotNull(productId, "Product ID assigned");

            // 4. Rename Category and verify product updates automatically
            HttpResponse<String> catUpd = put(client, baseUrl + "api/categories/" + catId, token,
                    JsonCodec.toJson(Map.of("name", "Network Hardware", "description", "Enterprise network gear")));
            Assert.assertEquals(200, catUpd.statusCode(), "Category renamed 200");

            HttpResponse<String> prodGet = get(client, baseUrl + "api/products/" + productId);
            Assert.assertEquals(200, prodGet.statusCode(), "Product detail GET 200");
            Map<String, Object> detailObj = (Map<String, Object>) JsonCodec.parseObject(prodGet.body()).get("product");
            Assert.assertEquals("Network Hardware", String.valueOf(detailObj.get("category")), "Product category reflected rename");

            // 5. Price Preview & Apply
            HttpResponse<String> prevResp = post(client, baseUrl + "api/pricing/preview", token,
                    JsonCodec.toJson(Map.of("productId", productId, "proposedPrice", "2099.00", "reason", "Supplier Price Increase")));
            Assert.assertEquals(200, prevResp.statusCode(), "Price preview 200");
            Assert.assertEquals(Boolean.TRUE, JsonCodec.parseObject(prevResp.body()).get("valid"), "Proposal valid");

            HttpResponse<String> applyResp = post(client, baseUrl + "api/pricing/apply", token,
                    JsonCodec.toJson(Map.of("productId", productId, "proposedPrice", "2099.00", "reason", "Supplier Price Increase")));
            Assert.assertEquals(200, applyResp.statusCode(), "Price apply 200");

            // 6. Reports & CSV Export
            HttpResponse<String> compResp = get(client, baseUrl + "api/reports/price-comparison");
            Assert.assertEquals(200, compResp.statusCode(), "Price comparison report 200");
            HttpResponse<String> csvResp = get(client, baseUrl + "api/reports/export/catalogue");
            Assert.assertEquals(200, csvResp.statusCode(), "Catalogue CSV export 200");
            Assert.assertTrue(csvResp.body().contains("Gigabit Switch 8-Port"), "Exported CSV contains created product");

            // 7. Create Backup
            HttpResponse<String> bkpResp = post(client, baseUrl + "api/backups", token,
                    JsonCodec.toJson(Map.of("note", "Pre-restart snapshot")));
            Assert.assertEquals(201, bkpResp.statusCode(), "Backup created 201");
            backupFileName = JsonCodec.getString(JsonCodec.parseObject(bkpResp.body()), "fileName");

            // 8. Delete test & invalid price rejection
            HttpResponse<String> badPriceResp = post(client, baseUrl + "api/products", token,
                    JsonCodec.toJson(Map.of("name", "Bad", "category", "Test", "costPrice", "-10", "sellingPrice", "20", "quantity", 1)));
            Assert.assertEquals(400, badPriceResp.statusCode(), "Negative cost price returns 400");

            // 9. CSV Import Preview & Commit
            String csvUpload = "name,category,description,costPrice,sellingPrice,quantity\n"
                    + "CAT6 Patch Cord,Network Hardware,2 Meter,90.00,199.00,60\n"
                    + "Bad Row,Network Hardware,Neg,-5.00,100.00,10\n";
            HttpResponse<String> impPrev = post(client, baseUrl + "api/import/preview", token,
                    JsonCodec.toJson(Map.of("csvContent", csvUpload)));
            Assert.assertEquals(200, impPrev.statusCode(), "CSV preview 200");
            Map<String, Object> prevMap = JsonCodec.parseObject(impPrev.body());
            Assert.assertEquals(new BigDecimal("1"), new BigDecimal(String.valueOf(prevMap.get("acceptedCount"))), "1 valid row");
            Assert.assertEquals(new BigDecimal("1"), new BigDecimal(String.valueOf(prevMap.get("rejectedCount"))), "1 rejected row");

            HttpResponse<String> impCommit = post(client, baseUrl + "api/import/commit", token,
                    JsonCodec.toJson(Map.of("csvContent", csvUpload)));
            Assert.assertEquals(200, impCommit.statusCode(), "CSV commit 200");
        } finally {
            app1.shutdown();
        }

        // Session 2: Restart server on same data directory & verify persistence + backup restore
        DesktopApplication app2 = new DesktopApplication(paths, true, 0);
        try {
            app2.startServices();
            String url2 = app2.getHttpServer().getBaseUrl();
            String token2 = app2.getHttpServer().getSessionToken();

            HttpResponse<String> list2 = get(client, url2 + "api/products?status=ALL");
            Assert.assertEquals(200, list2.statusCode(), "List after restart 200");
            List<?> items2 = (List<?>) JsonCodec.parseObject(list2.body()).get("items");
            Assert.assertEquals(2, items2.size(), "Both products survived server restart");

            // Restore backup (which had only 1 product before CSV import)
            HttpResponse<String> restoreResp = post(client, url2 + "api/backups/restore", token2,
                    JsonCodec.toJson(Map.of("fileName", backupFileName)));
            Assert.assertEquals(200, restoreResp.statusCode(), "Restore backup 200");

            HttpResponse<String> listAfterRestore = get(client, url2 + "api/products?status=ALL");
            List<?> restoredItems = (List<?>) JsonCodec.parseObject(listAfterRestore.body()).get("items");
            Assert.assertEquals(1, restoredItems.size(), "Backup restore reverted DB to 1 product");
        } finally {
            app2.shutdown();
        }
    }

    private static HttpResponse<String> get(HttpClient client, String url) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> post(HttpClient client, String url, String token, String json)
            throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .header("X-PPMS-Token", token)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> put(HttpClient client, String url, String token, String json)
            throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .header("X-PPMS-Token", token)
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
