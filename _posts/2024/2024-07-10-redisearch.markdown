---
layout: post
title:  "Redis for Search Engine"
date:   2024-07-10 21:54:00 +0900
categories: dev
---

# Intro
Redis는 캐시 서버로 많이 사용되고 있지만, 요즘엔 많은 변화가 있는 기술입니다. 이번엔 Redis의 새로운 부분에 대해서 포스팅합니다.

## SortedSet
Redis의 Sorted Set을 사용하여 실시간 검색어 시스템을 구현하는 방법은 효율적이면서도 실시간으로 데이터를 업데이트하고 관리할 수 있는 강력한 방법을 제공합니다. Redis Sorted Set은 값을 스코어에 따라 자동으로 정렬하는 데이터 구조입니다. 이를 이용하면, 검색어의 인기도나 빈도수를 스코어로 사용하여 실시간으로 검색어 순위를 관리할 수 있습니다.

1. Sorted Set 초기화 
    - 각 검색어를 Sorted Set에 추가할 때, 검색어가 키(Key)가 되고, 해당 검색어의 스코어(예를 들어 검색 횟수)를 값(Value)으로 합니다.
    - 사용자가 특정 단어를 검색할때마다, 해당 단어의 스코어를 증가시키기 위해 **ZINCRBY** 명령어를 사용합니다.
    
2. 실시간 검색어 순위 업데이트
    - ZINCRBY key increment member 명령어를 통해 검색의 스코어를 증가시킬수 있습니다.
        ``` 
        $ ZINCRBY realtime_search 1 "apple"
        $ ZINCRBY realtime_search 2 "banana"
        ```
3. 검색어 순위 조회
    - **ZREVRANGE** 명령어를 사용하여 스코어가 높은 순서대로 검색어를 조회할 수 있습니다.
        ```
        // 상위 10개의 검색어와 그 스코어를 반환합니다.
        $ ZREVRANGE realtime_search 0 9 WITHSCORES
        ```
4. 기간별 또는 조건별 순위 조정
    - 특정 기간 동안의 데이터만 유지하기 위해 검색어의 스코어를 주기적으로 초기화하거나, 오래된 검색어를 자동으로 제거할 수 있습니다.
    - EXPIRE 명령어를 이용해 자동으로 만료되도록 설정할 수도 있고, **ZREMRANGEBYSCORE**로 특정 스코어 범위의 검색어를 삭제할 수 있습니다.

![sortedSet](/assets/img/2024/240710/01.png)


### Sorted Set의 연산 속도
Redis의 Sorted Set은 내부적으로 스킵 리스트(skip list), 해시 맵(hash map), 그리고 때때로 ziplist(작은 데이터 세트에 대한 최적화를 위한 압축된 데이터 구조)를 사용하여 구현되어 있습니다. 이러한 구조는 데이터를 효율적으로 삽입, 삭제, 검색할 수 있게 해주며, 대부분의 작업은 로그 시간 복잡도(O(log N))를 가집니다.

#### 성능 상세
- 삽입 (ZADD): 새로운 요소를 추가하거나 기존 요소의 점수를 업데이트할 때, Sorted Set은 O(log N) 시간 복잡도를 가집니다. 여기서 N은 Sorted Set의 요소 수입니다.
- 삭제 (ZREM): 특정 요소를 삭제하는 연산 역시 O(log N)의 시간 복잡도를 가집니다.
- 검색 (ZRANK, ZSCORE 등): 요소의 순위를 찾거나 점수를 조회하는 연산은 O(log N)의 시간 복잡도로 수행됩니다.
- 범위 쿼리 (ZRANGE, ZREVRANGE 등): 특정 순위 범위에 있는 요소들을 조회할 때의 시간 복잡도는 O(log N + M)입니다. 여기서 M은 반환된 요소의 수입니다.

#### 스킵 리스트
스킵 리스트는 여러 레벨을 갖는 링크드 리스트 구조로, 노드들이 하나 이상의 레벨에서 다음 노드를 가리키는 포인터를 갖고 있습니다.

- 초기 상태가 노드 3,7,8,12로 구성된 스킵 리스트를 가정해보겠습니다.
    ```
    레벨 3: 헤드 -> 12
    레벨 2: 헤드 -> 7 -> 12
    레벨 1: 헤드 -> 3 -> 7 -> 8 -> 12
    ```

- 삽입과정: 값 10 삽입
    1. 위치 탐색:
        - 레벨 3에서 시작해 12보다 작은 10을 찾기 위해 레벨 2로 이동합니다.
        - 레벨 2에서 7 다음이 12이므로, 10을 삽입할 위치는 7과 12 사이임을 확인하고 레벨 1로 이동합니다.
        - 레벨 1에서 8 다음이 12이므로, 10은 8과 12 사이에 삽입됩니다.
    2. 레벨 결정:
        - 레벨 1에서 8 다음이 12이므로, 10은 8과 12 사이에 삽입됩니다.
    3. 노드 삽입 및 포인터 업그레이드:
        - 레벨 1에서 8의 다음 노드를 10으로 설정하고, 10의 다음 노드를 12로 설정합니다.
        - 레벨 2에서 7의 다음 노드를 10으로 설정하고, 10의 다음 노드를 12로 설정합니다.
        - 레벨 3은 변화 없음

    ```
    레벨 3: 헤드 -> 12
    레벨 2: 헤드 -> 7 -> 10 -> 12
    레벨 1: 헤드 -> 3 -> 7 -> 8 -> 10 -> 12
    ```

- 삭제과정: 값 8 삭제
    1. 위치 탐색:
        - 레벨 3에서 시작해서 8을 찾습니다.
        (삭제하려는 요소를 찾기 위해 가장 낮은 레벨에서 시작하는 것이 일반적입니다.)
    2. 노드 삭제 및 포인터 업데이트
        - 레벨 1에서 7의 다음 노드를 8에서 10으로 변경합니다.
    ```
    레벨 3: 헤드 -> 12
    레벨 2: 헤드 -> 7 -> 10 -> 12
    레벨 1: 헤드 -> 3 -> 7 -> 10 -> 12
    ```

### 스킵 리스트의 공간복잡도

- 포인터 저장: 스킵 리스트의 각 노드는 여러 레벨의 포인터를 저장해야 합니다. 일반적으로 노드 하나당 평균적으로 1/(1-p) 개의 포인터를 저장하게 됩니다. 여기서 p는 노드가 다음 레벨로 올라갈 확률입니다(예를 들어 p=0.5의 경우 평균적으로 각 노드는 2개의 포인터를 가집니다).

- 추가 메모리 요구: 스킵 리스트의 구조를 유지하기 위해 추가적인 메모리가 필요합니다. 이는 특히 높은 레벨의 노드에서 더 많은 포인터를 필요로 하며, 이 포인터들은 데이터 자체보다 많은 공간을 차지할 수 있습니다.

- 레벨의 수: 실제 메모리 사용량은 레벨의 수에 크게 의존합니다. 레벨이 많을수록 더 많은 포인터가 필요하고, 이는 공간 복잡도를 더 높게 만듭니다.

### AVL이나 RB트리랑 비교

- 포인터의 수: 스킵 리스트의 각 노드는 여러 레벨에 걸쳐 여러 포인터를 가질 수 있습니다. 이는 노드 하나당 여러 개의 포인터를 필요로 하며, 특히 높은 레벨의 노드는 많은 포인터를 가질 수 있습니다.

- 메모리 복잡도: AVL 트리와 레드-블랙 트리는 각각 높이가 로그 스케일로 증가하며, 상대적으로 균일한 높이를 유지하기 때문에 메모리 사용이 더 예측 가능하고 효율적입니다.


# RediSearch
> https://github.com/RediSearch/RediSearch

Redisearch는 Redis 데이터베이스에 텍스트 검색 및 인덱싱 기능을 추가하는 모듈입니다. Redis의 빠른 성능과 실시간 데이터 처리 능력을 활용하여 고성능 텍스트 검색을 제공합니다. Redisearch는 텍스트 기반의 검색뿐만 아니라 구조화된 데이터 검색, 범위 검색, 정렬, 필터링, 자동 완성 등의 고급 기능도 지원합니다.

주요 기능:
- 텍스트 검색: 자연어 처리, 부분 일치 검색, 정확한 구문 검색 등 다양한 텍스트 검색 기능.
- 구조화된 데이터 검색: 숫자, 날짜, 지리적 위치 등 다양한 데이터 타입을 지원.
- 고급 쿼리: AND, OR, NOT 연산자, 범위 검색, 정렬, 필터링 등 고급 쿼리 기능.
- 자동 완성 및 제안: 자동 완성 인덱스와 제안 기능.
- 실시간 검색: Redis의 고성능 실시간 데이터 처리 능력을 활용한 빠른 검색.

## Redisearch의 장점

1. **성능**: Redisearch는 Redis의 인메모리 데이터 저장소를 기반으로 하기 때문에 매우 빠른 검색 성능을 제공합니다. 이는 실시간 데이터 처리가 필요한 애플리케이션에 적합합니다. 특히, 짧은 응답 시간이 중요한 경우 Redisearch가 유리합니다.

2. **간편한 설치 및 사용**: Redisearch는 Redis 모듈로서, Redis 서버에 추가 설치가 쉽습니다. Redis 클라이언트 라이브러리를 통해 간단히 사용할 수 있으며, 복잡한 설정이 필요하지 않습니다.

3. **실시간 인덱싱**: Redisearch는 데이터가 추가되거나 업데이트될 때 실시간으로 인덱스를 갱신합니다. 이는 변경된 데이터를 즉시 검색할 수 있도록 하여 실시간 애플리케이션에 매우 유용합니다.

4. **낮은 메모리 오버헤드**: Redisearch는 Redis의 데이터 압축 및 효율적인 메모리 관리 기능을 활용하여 메모리 오버헤드를 최소화합니다. 이는 대용량 데이터 처리 시에도 효율적인 메모리 사용을 가능하게 합니다.

5. **통합된 데이터 저장**: Redis는 다양한 데이터 구조를 지원하며, Redisearch와 함께 사용하여 텍스트 검색, 캐싱, 메시지 브로커 등의 기능을 하나의 데이터 저장소에서 통합적으로 관리할 수 있습니다.

## RediSearch vs ElasticSearch
> https://redis.io/blog/search-benchmarking-redisearch-vs-elasticsearch/

- Redisearch: 실시간 검색, 낮은 지연 시간, 간단한 설정이 필요한 경우 적합합니다. 특히 Redis를 이미 사용하고 있는 환경에서는 쉽게 통합할 수 있습니다.

- Elasticsearch: 대규모 데이터 세트, 복잡한 쿼리 및 분석, 높은 확장성이 필요한 경우 적합합니다. 로그 분석, 데이터 시각화, 풀텍스트 검색 등 다양한 사용 사례에 유리합니다.

## 예시 코드

~~~ js
const Redis = require('ioredis');

const redis = new Redis({
  host: 'localhost', // Redis 서버 호스트
  port: 6379        // Redis 서버 포트
});

(async () => {
  // 인덱스가 존재하는지 확인하는 함수
  const indexExists = async (indexName) => {
    try {
      await redis.call('FT.INFO', indexName);
      return true;
    } catch (err) {
      return false;
    }
  };

  const createIndex = async (indexName) => {
    const exists = await indexExists(indexName);
    if (!exists) {
      await redis.call('FT.CREATE', indexName,
        'ON', 'HASH',
        'PREFIX', '1', 'doc:',
        'SCHEMA',
        'title', 'TEXT',
        'content', 'TEXT'
      );
      console.log('Index created.');
    } else {
      console.log('Index already exists.');
    }
  };

  // 인덱스 생성 (기존에 있는 경우 생략)
  await createIndex('myIndex');

  // 문서 추가 (업데이트 포함)
  const addOrUpdateDocument = async (docId, fields) => {
    await redis.hmset(docId, fields);
    console.log(`Document ${docId} added or updated.`);
  };

  // 문서 추가
  await addOrUpdateDocument('doc:1', {
    title: 'Hello World',
    content: 'This is the content of the first document. It has some example text.'
  });

  await addOrUpdateDocument('doc:2', {
    title: 'Redisearch Example',
    content: 'This is an example of using Redisearch with Node.js. Redisearch is powerful.'
  });

  await addOrUpdateDocument('doc:3', {
    title: 'Example Title',
    content: 'Another example of a document with the title containing example.'
  });

  await addOrUpdateDocument('doc:4', {
    title: 'Hello Redisearch',
    content: 'Document with a title that starts with Hello.'
  });
  
  const countTitlesWithSubstring = async (substring) => {
    const query = `@title:*${substring}*`;
    const aggregateResult = await redis.call(
      'FT.AGGREGATE', 'myIndex', query,
      'GROUPBY', '0', 'REDUCE', 'COUNT', '0', 'AS', 'count'
    );

    return aggregateResult[1];
  };

   const searchDocumentsInTitle = async (word) => {
    const searchResult = await redis.call('FT.SEARCH', 'myIndex', `@title:${word}*`);
    console.log(searchResult);

    return searchResult;
  };

  // 'example'을 포함하는 title의 개수 집계
  const substringToCount = 'example';  // ex*
  const count = await countTitlesWithSubstring(substringToCount);
  console.log(count);


  const wordToSearch = 'ex';
  await searchDocumentsInTitle(wordToSearch);

  redis.quit();
})();
~~~

![rediSearch](/assets/img/2024/240710/02.png)

## 고급 쿼리기능
- AND, OR, NOT 연산자: 복잡한 논리적 조건을 설정할 수 있습니다.
    ``` js
    const searchResult = await redis.call('FT.SEARCH', 'myIndex', 'example | redisearch -powerful');
    ```
- 정확한 구문 검색: 따옴표를 사용하여 정확한 구문을 검색할 수 있습니다.
    ``` js
    const searchResult = await redis.call('FT.SEARCH', 'myIndex', '"example of using Redisearch"');
    ```
- 범위 검색: 숫자, 날짜 등의 범위를 검색할 수 있습니다.
    ``` js
    await redis.call('FT.CREATE', 'myIndex', 'ON', 'HASH', 'PREFIX', '1', 'doc:', 'SCHEMA', 'price', 'NUMERIC');
    const searchResult = await redis.call('FT.SEARCH', 'myIndex', '@price:[10 100]');
    ```
- 필터링: 특정 필드 값으로 결과를 필터링할 수 있습니다.
    ``` js
    const searchResult = await redis.call('FT.SEARCH', 'myIndex', '@title:example @content:Redisearch');
    ```
- 정렬: 결과를 특정 필드 기준으로 정렬할 수 있습니다.
    ``` js
    const searchResult = await redis.call('FT.SEARCH', 'myIndex', 'example', 'SORTBY', 'price', 'ASC');
    ```
- 집계: 검색 결과를 그룹화하고 통계를 계산할 수 있습니다.
    ``` js
    const aggregateResult = await redis.call('FT.AGGREGATE', 'myIndex', '*', 'GROUPBY', '1', '@title', 'REDUCE', 'COUNT', '0', 'AS', 'count');
    ```
- 자동완성 인덱스 생성: 자동완성을 위한 인덱스를 생성할 수 있습니다.
    ``` js
    await redis.call('FT.SUGADD', 'autoComplete', 'redisearch', '1');
    ```
- 자동완성 제안: 사용자가 입력한 텍스트에 대해 자동완성 제안을 받을 수 있습니다.
    ``` js
    const suggestions = await redis.call('FT.SUGGET', 'autoComplete', 'red', 'FUZZY');
    ```

