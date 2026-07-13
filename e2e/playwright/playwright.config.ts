import { defineConfig } from '@playwright/test';

export default defineConfig({
    testDir: './tests',
    fullyParallel: false,
    workers: 1,
    reporter: 'list',
    timeout: 60_000,
    // Sem `use.extraHTTPHeaders` de propósito (desvio do plano original, achado durante
    // execução local): um Content-Type fixo aqui se aplicaria também à chamada de token do
    // Keycloak, que usa `form:` (application/x-www-form-urlencoded) - forçar
    // application/json nela quebra o parse do Keycloak (400 invalid_request, "Missing form
    // parameter: grant_type"). O APIRequestContext do Playwright já infere o Content-Type
    // correto por chamada (JSON para `data`, urlencoded para `form`), então não precisa
    // (nem pode) ser fixado globalmente.
});
