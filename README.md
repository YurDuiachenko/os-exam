# Микросервисы: пример архитектуры и инфраструктуры

В последнее время, с усложнением веб-сервисов всё более актуальной и даже необходимой становится микросервисная архитектура.
Она отличается высокой отказоустойчивостью, масштабированием и сложностью проектирования и поддержки. 

Рассмотрим небольшой пример, демонстрирующий современные технологии интеграции между сервисами (REST, gRPC, RabbitMQ),
а также современные стэк, окружающий эти сервисы кэшированием (Redis), мониторингом (Prometheus Stack) и логированием (ELK).

## Gateway
Сервис, который принимает HTTP-запросы от клиентов и маршрутизирует их к соответствующим внутренним сервисам.
Написан на **Java и Spring Boot**. 

Состоит из:
 - /config - конфигурации Redis и RabbitMQ;
 - /redis - вспомогательные файлы для Redis;
 - /rest - контроллеры и dto для общения с клиентом, тут происходит перенаправление на внутренние сервисы;

Слой /service не нужен, так как никакой бизнес-логики сервис не реализует.

Вся конфигурация сервиса лежит в `/gateway/src/main/resources/application.yaml`.
Все точки взаимодействия прописаны, как переменные окружения. Это обеспечивает гибкость при запуске.

Чтобы контейнеризировать сервис нужен Dockerfile:
```dockerfile
FROM openjdk:17-jdk-alpine

WORKDIR /app

COPY build/libs/gateway-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

Собираем сервис, собираем образ, тэгируем и пушим на наш докерхаб:
```shell
./gradlew clean build -x test
docker build -t gateway .
docker tag gateway yuridiachenko/gateway:latest
docker push yuridiachenko/gateway:latest
```
Не забудьте выполнить перед этим `docker login`!

Поднимаем в docker-compose, прокидывая стандартные порты и переменные окружения,
а также прокинем из контейнера файл с логам (пригодится позже).
```yaml
gateway:
  image: yuridiachenko/gateway:latest
  pull_policy: always
  container_name: gateway
  ports:
    - "8080:8080"
  volumes:
    - ./logstash/logs/application.log:/app/logs/application.log
  environment:
    - RABBITMQ_HOST=rabbitmq
    - RABBITMQ_PORT=5672
    - REDIS_HOST=redis
    - REDIS_PORT=6379
    - GRPC_SERVER_HOST=domain
    - GRPC_SERVER_PORT=50051
  depends_on:
    - domain
  restart: always
```

## Domain
Cервис, предоставляющий API для выполнения операций создания, чтения, обновления и удаления данных (CRUD). 
Взаимодействовать с шлюзом через gRPC-протокол и шину данных.
Написан на **Java и Spring Boot**.

Состоит из:
- /config - конфигурации gRPC и RabbitMQ;
- /domain - доменный слой;
- /grpc - файлы для работы с gRPC;
- /redis - файлы для работы с RabbitMQ;
- /repository - репозиторий для CRUD операций работы с MongoDB;

Слой /service не нужен, так как никакой бизнес-логики сервис не реализует, только CRUD.

Вся конфигурация сервиса лежит в `/domain/src/main/resources/application.yaml`.
Все точки взаимодействия прописаны, как переменные окружения. Это обеспечивает гибкость при запуске.

Чтобы контейнеризировать сервис нужен Dockerfile:
```dockerfile
FROM openjdk:17-jdk-alpine

WORKDIR /app

COPY build/libs/domain-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8082
EXPOSE 50051

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

Собираем сервис, собираем образ, тэгируем и пушим на наш докерхаб аналогично со шлюзом.

Поднимаем в docker-compose, прокидывая стандартные порты и переменные окружения.
```yaml
domain:
  image: yuridiachenko/domain:latest
  pull_policy: always
  container_name: domain
  ports:
    - "8082:8082"
    - "50051:50051"
  environment:
    - RABBITMQ_HOST=rabbitmq
    - RABBITMQ_PORT=5672
    - MONGODB_HOST=mongo
    - MONGODB_PORT=27017
    - GRPC_SERVER_PORT=50051
  depends_on:
    - mongo
    - rabbitmq
    - redis
```


## База данных
В качестве базы данных используется **MongoDB**, а также инструмент к нему MongoExpress. (Можно заменить на PostgreSQL и PgAdmin)

Поднимаем в docker-compose, прокидывая стандартные порты, следя за тем, чтобы экспресс поднялся только после монго через depends_on:
```yaml
mongo:
  image: mongo:7
  container_name: mongo
  ports:
    - "27017:27017"

mongo-express:
  image: mongo-express:latest
  container_name: mongo-express
  depends_on:
    - mongo
  ports:
    - "8081:8081"
```

## Кэширование
Реализовано через **Redis** и  Java RedisClient и происходит в шлюзе. Кэшируются GET-запросы.

Поднимаем в docker-compose, прокидывая стандартные порты:
```yaml
redis:
  image: "redis:6.2-alpine"
  container_name: redis
  ports:
    - "6379:6379"
```

## Шина данных
В качестве шины данных используется **RabbitMQ**, а также инструмент к нему RabbitManagement.
Помогает реализовывать асинхронные операции создания, обновления, удаления.

Поднимаем в docker-compose, прокидывая стандартные порты:
```yaml
rabbitmq:
  image: rabbitmq:management
  container_name: rabbitmq
  ports:
    - "5672:5672"
    - "15672:15672"
  restart: always
```

## Мониторинг 
Реализован через **Promethues и Grafana**, а также с помощью специальной библиотеки Micrometer, 
```groovy
dependencies {
    //metrics
    implementation 'io.micrometer:micrometer-registry-prometheus:1.12.3'
    // ...
}
```
которая собирает метрики с шлюза и предоставляет их на эндпоинт `/actuator/prometheus`.

Prometheus собирает их раз в 15 секунд с этого энпоинта и сохраняет в своей СУБД временных рядов. 
Конфигурация находится [тут](/prometheus/prometheus.yml), и выглядит это вот так:
```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'gateway'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:8080']
```

Grafana использует Prometheus как источник данных, для этого нужно сконфигурировать его таковым
как [тут](/grafana/provisioning/datasources/datasource.yml), и выглядит это вот так:
```yaml
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: true
```

Теперь у нас есть хранилище метрик, к которому мы имеем доступ через Grafana Datasources.
Настроим дашборды. Это можно сделать и руками через UI (как и datasource), 
но это долго и пришлось бы делать каждый раз заново. Поэтому мы просто сконфигурируем их,
указав к ним путь, тип и другие параметры как [тут](/grafana/provisioning/dashboards/dashboard.yaml) 
и выглядит это вот так:
```yaml
providers:
  - name: 'default'
    orgId: 1
    folder: ''
    type: file
    disableDeletion: false
    updateIntervalSeconds: 10
    options:
      path: /etc/grafana/provisioning/dashboards
```
Далее лениво заходим на Grafana Labs и выбираем понравившиеся нам дашборды. 
У меня это [Spring Boot HTTP](https://grafana.com/grafana/dashboards/20820-spring-boot-http/) и 
[Spring Boot 2.1 Statistics](https://grafana.com/grafana/dashboards/11378-justai-system-monitor/).
Сохраняем понравившиеся дашборды в JSON рядом с конфигурацией - и всё.

Единственный момент - стоит проследить, чтобы название датасорса в дашборде соответствовало сконфигурированному ранее.

Поднимаем в docker-compose, прокидывая стандартные порты, следя за тем, 
чтобы наша система мониторинга поднялась только после шлюза, 
также не забываем прокинуть в контейнер все файлы конфигурации, 
которые написали, через volumes:
```yaml
prometheus:
  image: prom/prometheus:latest
  container_name: prometheus
  volumes:
    - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
  ports:
    - "9090:9090"
  depends_on:
    - gateway

grafana:
  image: grafana/grafana:latest
  container_name: grafana
  ports:
    - "3000:3000"
  volumes:
    - ./grafana/provisioning:/etc/grafana/provisioning
  environment:
    - GF_SECURITY_ADMIN_USER=admin
    - GF_SECURITY_ADMIN_PASSWORD=admin
  depends_on:
    - prometheus
```

## Логирование
Реализовано через **Elastic, Logstash и Kibana** или коротко **ELK**,
а также библиотеки для логирования Logback. 

Заморачиваться ни о чём не нужно, Logback входит в Spring Boot стандарт и 
обычно вместе со своим другом log4j скрывается за аннотацией `@Slf4j`, которую мы используем в шлюзе.

Нам нужно только добавить аппендер рядом с конфигурацией проекта, 
так чтобы он выводил все логи помимо консоли ещё и в файл, 
как [тут](/gateway/src/main/resources/logback.xml), и выглядит это вот так:
```xml
<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/application.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
        <fileNamePattern>logs/application.%d{yyyy-MM-dd}.log</fileNamePattern>
        <maxHistory>30</maxHistory>
    </rollingPolicy>
    <encoder>
        <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
</appender>
```
Логи теперь есть в `/gateway/logs/application.log`. При создании шлюза мы уже прокинули 
этот файл наружу контейнера в `/logstash/logs/application.log`, для того,
чтобы сейчас положить его в контейнер Logstash.

Сконфигурируем Logstash так, чтобы он читал логи из указанного нами файла и отправлял их в ElasticSearch,
как [тут](/logstash/logstash.conf), и выглядит это вот так:
```logstash
input {
  file {
    path => "/app/logs/application.log"
    start_position => "beginning"
    sincedb_path => "/dev/null"
  }
}

# ...

output {
  elasticsearch {
    hosts => ["host.docker.internal:9200"]
    index => "gateway-logs-%{+YYYY.MM.dd}"
  }

  stdout { codec => rubydebug }
}
```

Поднимаем в docker-compose ElasticSearch и Logstash, прокидывая стандартные порты,
также не забываем прокинуть в контейнер все файлы конфигурации, которые написали:
```yaml
elasticsearch:
  image: docker.elastic.co/elasticsearch/elasticsearch:7.10.1
  container_name: elasticsearch
  environment:
    - discovery.type=single-node
    - xpack.security.enabled=false
  ports:
     - "9200:9200"

logstash:
  image: docker.elastic.co/logstash/logstash:7.10.1
  container_name: logstash
  volumes:
    - ./logstash/logstash.conf:/usr/share/logstash/pipeline/logstash.conf
    - ./logstash/logs/application.log:/app/logs/application.log
  depends_on:
    - elasticsearch
  ports:
    - "5044:5044"
    - "5000:5000"
    - "9600:9600"
```

Осталось отобразить эти логи в Kibana. Чтобы это сделать поднимем её в docker-compose.
```yaml
kibana:
  image: docker.elastic.co/kibana/kibana:7.10.1
  container_name: kibana
  environment:
    - ELASTICSEARCH_HOSTS=http://elasticsearch:9200
  ports:
    - "5601:5601"
  depends_on:
    - elasticsearch
```

Логи нужно индексировать, это можно сделать через UI, но я не хочу :) ,
поэтому создадим индекс `gateway-logs-*` с меткой по времени, 
обратившись к API Kibana через [вот такой](/kibana/init/init.sh) bash скрипт:
```shell
curl -X POST "http://localhost:5601/api/saved_objects/index-pattern/gateway-logs-*" \
     -H "kbn-xsrf: true" \
     -H "Content-Type: application/json" \
     -d '{
           "attributes": {
             "title": "gateway-logs-*",
             "timeFieldName": "@timestamp"
           }
         }'
```
Теперь логи можно смотреть и делать по ним запросы в Kibana Discovery.

## Запуск

Все указанные выше docker-compose компоненты объединены в `docker-compose.yaml`
и поднять их все можно одной командой:
```shell
docker compose up -d
```
Не забудем про последний шаг настройки логирования:
```shell
cd kibana
cd init
./init.sh
```
И всё - система готова к работе!

## Тестирование
 * http://localhost:8080/book/all - покидать запросы на шлюз через Postman, 
бразуер или как [тут](testing/gateway.http), через особый файл;
 * http://localhost:8081/ - посмотреть работу MongoDB (логин: admin, пароль: pass);
 * http://localhost:15672/#/ - посмотреть работу RabbitMQ (логин: guest, пароль: guest);
 * http://localhost:8080/actuator/metrics - посмотреть, какие метрики отдаёт шлюз;
 * http://localhost:8080/actuator/prometheus - посмотреть сами метрики;
 * http://localhost:9090/query - поделать запросы к Prometheus;
 * http://localhost:3000/dashboards - посмотреть дашборды в Grafana (логин: admin, пароль: admin);
 * http://localhost:5601/app/discover - - посмотреть логи в Kibana;
