# api-gateway

Single entry point for routing traffic to microservices.

## Browser access and CORS

- Global CORS is configured in `infrastructure/config-repo/api-gateway.yml`.
- Initial allowed origin: `http://localhost:5173`.
- Preflight `OPTIONS` enabled via `globalcors.add-to-simple-url-handler-mapping: true`.
- Reactive security enables CORS in `GatewaySecurityConfig` with `http.cors(...)`.

## Quick verification

```bash
curl -i -X OPTIONS http://localhost:8080/auth/login \
  -H "Origin: http://localhost:5173" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: authorization,content-type"
```

Expect: `Access-Control-Allow-Origin`, `Access-Control-Allow-Methods`, and `Access-Control-Allow-Headers` headers in the response.
