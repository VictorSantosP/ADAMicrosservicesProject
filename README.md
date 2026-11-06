# Projeto Final — Gateway de API Seguro para Microserviços

## 1. Sobre o projeto

Este projeto consiste na implementação de um **API Gateway seguro para uma arquitetura de microsserviços**, desenvolvido utilizando Java e Spring Boot.

O objetivo é centralizar a autenticação, autorização e aplicação de políticas de segurança em um único ponto de entrada, protegendo os serviços internos contra acessos não autorizados.

A solução utiliza dois serviços independentes:

- **gatewayMS:** responsável por receber as requisições externas, validar tokens JWT, aplicar regras de autorização e encaminhar as requisições ao serviço interno.
- **productsMS:** microsserviço responsável por disponibilizar os dados dos produtos, protegido contra acessos diretos não autorizados.

O projeto utiliza uma implementação simplificada, com produtos armazenados em memória, sem necessidade de banco de dados.

## 2. Tecnologias utilizadas

- Java 25
- Spring Boot 4.1.2
- Spring Security
- Spring Cloud Gateway
- Spring WebFlux
- Spring MVC
- JWT (JSON Web Token)
- JJWT 0.12.6
- Maven
- Postman
- HTTPS com certificado PKCS12

## 3. Arquitetura

A arquitetura foi organizada para que todas as requisições externas passem pelo API Gateway antes de alcançar os microsserviços internos.

```text
                 CLIENTE
                    |
                    | HTTPS + JWT
                    v
          +----------------------+
          |       gatewayMS      |
          |                      |
          |  JWT Authentication  |
          |  RBAC Authorization  |
          |  Rate Limiting       |
          |  Security Headers    |
          |  Error Handling      |
          +----------------------+
                    |
                    | HTTP (ambiente local)
                    | X-Internal-Secret
                    v
          +----------------------+
          |      productsMS      |
          |                      |
          |  Internal Security   |
          |  Product Service     |
          +----------------------+
                    |
                    v
              Lista de produtos
```

### Portas utilizadas

| Serviço | Porta | Protocolo |
|---|---:|---|
| gatewayMS | 8443 | HTTPS |
| productsMS | 8081 | HTTP |

O Gateway funciona como ponto de entrada da aplicação. O microsserviço de produtos não deve ser acessado diretamente por clientes externos.

## 4. Estrutura do projeto

O projeto possui dois módulos independentes, cada um com seu próprio `pom.xml`.

### 4.1. gatewayMS

```text
gatewayMS/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/example/gatewayMS/
│       │       ├── exceptions/
│       │       │   ├── ApiErrorResponse.java
│       │       │   ├── GlobalErrorWebExceptionHandler.java
│       │       │   └── SecurityErrorHandler.java
│       │       │
│       │       ├── gateway/
│       │       │
│       │       ├── infrastructure/
│       │       │   └── security/
│       │       │       ├── configuration/
│       │       │       │   └── SecurityConfig.java
│       │       │       └── filter/
│       │       │           └── RateLimitGlobalFilter.java
│       │       │
│       │       └── GatewayMsApplication.java
│       │
│       └── resources/
│           ├── application.properties
│           ├── application.yml
│           └── gateway-local.p12
│
└── pom.xml
```

Principais responsabilidades:

- `SecurityConfig`: configura autenticação JWT, autorização RBAC, CSRF, headers de segurança e tratamento de acesso negado.
- `RateLimitGlobalFilter`: implementa o limite de requisições por usuário ou endereço IP.
- `ApiErrorResponse`: define o formato padronizado das respostas de erro.
- `GlobalErrorWebExceptionHandler`: realiza o tratamento global de exceções do WebFlux.
- `SecurityErrorHandler`: padroniza as respostas de autenticação e autorização.
- `gateway`: contém os componentes relacionados ao roteamento das requisições.

### 4.2. productsMS

```text
productsMS/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/example/productsMS/
│       │       ├── domain/
│       │       │
│       │       ├── infrastructure/
│       │       │   └── security/
│       │       │
│       │       ├── resources/
│       │       │   ├── controller/
│       │       │   │   └── ProductController.java
│       │       │   │
│       │       │   ├── exceptions/
│       │       │   │   ├── ApiErrorResponse.java
│       │       │   │   ├── ErrorBuilder.java
│       │       │   │   └── GlobalHandlerException.java
│       │       │   │
│       │       │   └── services/
│       │       │       └── ProductService.java
│       │       │
│       │       └── ProductsMsApplication.java
│       │
│       └── resources/
│           └── application.properties
│
└── pom.xml
```

Principais responsabilidades:

- `domain`: contém as entidades e os modelos do domínio.
- `infrastructure.security`: contém os componentes responsáveis pela proteção do microsserviço.
- `resources.controller`: disponibiliza os endpoints da API.
- `resources.services`: contém a lógica de negócio dos produtos.
- `resources.exceptions`: contém o tratamento de exceções e a padronização de erros.
- `ProductsMsApplication`: classe principal da aplicação.

## 5. Segurança e funcionalidades implementadas

### 5.1. Autenticação JWT

A autenticação é realizada pelo Gateway utilizando tokens JWT assinados com o algoritmo HMAC SHA-256 (HS256).

O Gateway valida:

- Assinatura do token.
- Data de expiração (`exp`).
- Emissor (`iss`).

O emissor configurado para o ambiente de desenvolvimento é:

```text
http://localhost:9000
```

As requisições protegidas devem enviar o token no cabeçalho:

```http
Authorization: Bearer SEU_TOKEN
```

Tokens ausentes, inválidos, expirados ou com assinatura incorreta são rejeitados com HTTP 401 Unauthorized.

### 5.2. Autorização baseada em permissões (RBAC)

Além de autenticar o usuário, o Gateway verifica as permissões presentes no JWT.

Foram definidas duas permissões:

- `products:read`: permite consultar produtos.
- `products:write`: destinada às operações de escrita.

As regras de autorização são:

| Operação | Permissão exigida |
|---|---|
| GET /api/produtos | products:read |
| GET /api/produtos/** | products:read |
| POST /api/produtos/** | products:write |
| PUT /api/produtos/** | products:write |
| DELETE /api/produtos/** | products:write |

Caso o usuário esteja autenticado, mas não possua a permissão necessária, o Gateway retorna HTTP 403 Forbidden.

### 5.3. Proteção do microsserviço interno

O `productsMS` possui uma camada adicional de segurança para impedir chamadas diretas sem autenticação interna.

O Gateway injeta o cabeçalho:

```http
X-Internal-Secret: SEGREDO_INTERNO
```

O backend verifica esse valor antes de permitir o acesso aos recursos.

A comparação do segredo utiliza `MessageDigest.isEqual`, evitando uma comparação simples de strings.

Dessa maneira, conhecer o endereço do microsserviço não é suficiente para acessar seus recursos.

### 5.4. Proteção contra CSRF e sessões

As aplicações utilizam uma abordagem stateless.

O Gateway não depende de sessões para autenticar usuários. A autenticação é realizada por meio do JWT enviado em cada requisição.

O CSRF foi desabilitado nas configurações de segurança, considerando que a API utiliza autenticação por Bearer Token e não autenticação baseada em cookies de sessão.

### 5.5. Proteção contra XSS e cabeçalhos HTTP

Foram configurados cabeçalhos de segurança utilizando o Spring Security.

Entre eles:

- Content-Security-Policy (CSP).
- X-Content-Type-Options.
- X-Frame-Options.
- Referrer-Policy.
- X-XSS-Protection.

A política CSP foi configurada de maneira restritiva, considerando que o Gateway disponibiliza uma API JSON, e não páginas HTML.

O cabeçalho legado X-XSS-Protection permanece desabilitado (`0`), pois a proteção moderna é realizada principalmente por meio de CSP e boas práticas de tratamento de conteúdo.

### 5.6. Rate Limiting

Foi implementado um filtro global no Gateway para limitar o número de requisições.

Configuração atual:

- Limite: 60 requisições.
- Janela de tempo: 60 segundos.
- Identificação: usuário autenticado ou endereço IP para requisições anônimas.

Quando o limite é ultrapassado, o Gateway retorna:

```http
429 Too Many Requests
```

A resposta também contém o cabeçalho `Retry-After`, informando quantos segundos devem ser aguardados antes de uma nova tentativa.

A implementação utiliza `ConcurrentHashMap` e mantém os contadores em memória.

### 5.7. HTTPS e HSTS

O Gateway foi configurado para receber requisições utilizando HTTPS.

Foi gerado um certificado local no formato PKCS12, utilizado pelo servidor para estabelecer conexões criptografadas.

O HSTS foi configurado com validade de 365 dias:

```http
Strict-Transport-Security: max-age=31536000
```

O certificado utilizado é autoassinado e destinado exclusivamente ao ambiente de desenvolvimento.

Em produção, deve ser substituído por um certificado emitido por uma autoridade certificadora confiável.

### 5.8. Tratamento global de exceções

Foi implementado um mecanismo global de tratamento de erros compatível com Spring WebFlux.

As respostas de autenticação e autorização também possuem tratamento específico.

Foi criado um modelo padronizado de resposta:

```json
{
  "timestamp": "2026-11-06T13:30:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Autenticação necessária ou token inválido.",
  "path": "/api/produtos"
}
```

Os erros não devem expor informações internas, stack traces, tokens ou detalhes sensíveis da implementação.

## 6. Como executar o projeto

### Pré-requisitos

- Java JDK 25.
- IntelliJ IDEA ou outra IDE compatível.
- Maven.
- Postman para os testes das APIs.

### Passo 1 — Abrir o projeto

Abra a pasta `AdaGatewayProject` no IntelliJ IDEA.

Os módulos `gatewayMS` e `productsMS` possuem seus próprios arquivos `pom.xml` e devem ser reconhecidos como aplicações independentes.

Aguarde o Maven baixar e sincronizar as dependências.

### Passo 2 — Executar o productsMS

Abra a classe principal:

```text
productsMS/src/main/java/com/example/productsMS/ProductsMsApplication.java
```

Execute a aplicação.

O serviço ficará disponível localmente na porta:

```text
http://localhost:8081
```

### Passo 3 — Executar o gatewayMS

Abra a classe principal:

```text
gatewayMS/src/main/java/com/example/gatewayMS/GatewayMsApplication.java
```

Execute a aplicação.

O Gateway ficará disponível em:

```text
https://localhost:8443
```

O Gateway utiliza o certificado local `gateway-local.p12`, incluído nos recursos do projeto.

### Passo 4 — Gerar o token JWT

Na raiz do projeto, execute o arquivo:

```text
gerar-token-jwt.bat
```

Esse arquivo automatiza a geração de um JWT para os testes.

O token gerado deve ser copiado e utilizado no Postman como Bearer Token.

O token de teste possui a permissão `products:read`, permitindo consultar a lista de produtos.

**Importante:** o token é temporário e possui prazo de expiração. Caso expire, basta executar novamente o arquivo BAT para gerar outro.

Se o arquivo BAT utilizar um script auxiliar `.ps1`, mantenha esse arquivo junto com ele, conforme a estrutura esperada pelo próprio BAT.

### Passo 5 — Configurar o Postman

Como o certificado HTTPS é autoassinado, pode ser necessário desativar a verificação de certificados SSL no Postman para os testes locais.

Para isso:

1. Abra Settings.
2. Acesse General.
3. Localize SSL certificate verification.
4. Desative a opção para o ambiente local de testes.

Essa configuração não deve ser utilizada como prática de produção.

## 7. Endpoints disponíveis

### Consultar produtos

```http
GET https://localhost:8443/api/produtos
```

Autenticação:

```http
Authorization: Bearer SEU_TOKEN
```

Resultado esperado:

```http
200 OK
```

O Gateway valida o JWT, verifica a permissão `products:read` e encaminha a requisição ao `productsMS`.

## 8. Testes realizados

Os testes foram realizados manualmente utilizando o Postman, verificando os cenários de sucesso e falha.

| Cenário | Resultado esperado | Situação |
|---|---|---|
| Requisição com JWT válido e permissão correta | 200 OK | Validado |
| Requisição sem JWT | 401 Unauthorized | Validado |
| JWT válido, mas sem permissão de leitura | 403 Forbidden | Validado |
| Acesso direto ao backend sem segredo interno | 401 Unauthorized | Validado |
| Excesso de requisições por minuto | 429 Too Many Requests | Validado |
| Comunicação HTTPS com o Gateway | HTTPS funcional | Validado |
| Cabeçalhos de segurança | Cabeçalhos presentes | Validado |

### Exemplo de teste de autorização

Um token válido, mas sem a permissão `products:read`, deve resultar em:

```http
403 Forbidden
```

Isso demonstra a separação entre autenticação e autorização: o usuário pode estar autenticado e ainda assim não possuir autorização para executar determinada operação.

### Exemplo de teste de Rate Limiting

Ao ultrapassar 60 requisições dentro da janela de 60 segundos, o Gateway bloqueia novas requisições e retorna:

```http
429 Too Many Requests
```

## 9. Decisões de arquitetura

A implementação prioriza a centralização das políticas de segurança no API Gateway, evitando que cada microsserviço precise implementar individualmente toda a lógica de autenticação externa.

O `productsMS` mantém uma camada adicional de proteção para garantir que as chamadas recebidas sejam provenientes de um componente autorizado.

A separação entre os serviços também permite que novos microsserviços sejam adicionados futuramente, utilizando o Gateway como ponto central de entrada e controle de acesso.

## 10. Limitações e considerações para produção

O projeto foi desenvolvido com finalidade acadêmica e demonstração de conceitos de segurança aplicados a uma arquitetura de microsserviços.

Algumas configurações devem ser aprimoradas antes de uma implantação real:

- Substituir os segredos de desenvolvimento por variáveis de ambiente ou um gerenciador seguro de segredos.
- Utilizar certificado TLS emitido por autoridade certificadora confiável.
- Proteger também a comunicação interna entre os serviços com TLS ou uma rede privada adequadamente protegida.
- Substituir o armazenamento em memória do Rate Limiting por uma solução distribuída, como Redis, em ambientes com múltiplas instâncias.
- Implementar mecanismos de limpeza dos contadores antigos do Rate Limiting.
- Implementar persistência de dados no microsserviço de produtos.
- Adicionar testes automatizados unitários e de integração.

As chaves e senhas presentes nas configurações atuais são exclusivamente para desenvolvimento local e não devem ser reutilizadas em produção.

## 11. Conclusão

O projeto demonstra a implementação de um API Gateway capaz de centralizar autenticação JWT, autorização baseada em permissões, proteção dos microsserviços internos, limitação de requisições e comunicação HTTPS.

Também foram implementados mecanismos de proteção contra CSRF, políticas de segurança relacionadas a XSS, HSTS e tratamento padronizado de erros.

A solução atende aos principais requisitos propostos, utilizando componentes do ecossistema Spring para construir uma arquitetura de microsserviços com controle centralizado de acesso e segurança.