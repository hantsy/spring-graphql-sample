





## DataLoaders



```bash

2021-06-14 13:04:46.658 DEBUG 22384 --- [nio-8080-exec-4] o.s.jdbc.datasource.DataSourceUtils      : Fetching JDBC Connection from DataSource
2021-06-14 13:04:46.675 DEBUG 22384 --- [nio-8080-exec-4] o.s.jdbc.core.JdbcTemplate               : Executing prepared SQL query
2021-06-14 13:04:46.675 DEBUG 22384 --- [nio-8080-exec-4] o.s.jdbc.core.JdbcTemplate               : Executing prepared SQL statement [SELECT * FROM comments
WHERE post_id  = ?
]
2021-06-14 13:04:46.675 DEBUG 22384 --- [nio-8080-exec-4] o.s.jdbc.datasource.DataSourceUtils      : Fetching JDBC Connection from DataSource
2021-06-14 13:04:46.681 DEBUG 22384 --- [nio-8080-exec-4] o.s.jdbc.core.JdbcTemplate               : Executing prepared SQL query
2021-06-14 13:04:46.681 DEBUG 22384 --- [nio-8080-exec-4] o.s.jdbc.core.JdbcTemplate               : Executing prepared SQL statement [SELECT * FROM comments
WHERE post_id  = ?
]
2021-06-14 13:04:46.681 DEBUG 22384 --- [nio-8080-exec-4] o.s.jdbc.datasource.DataSourceUtils      : Fetching JDBC Connection from DataSource
2021-06-14 13:04:46.713 DEBUG 22384 --- [nio-8080-exec-4] o.s.jdbc.core.JdbcTemplate               : Executing prepared SQL query
2021-06-14 13:04:46.714 DEBUG 22384 --- [nio-8080-exec-4] o.s.jdbc.core.JdbcTemplate               : Executing prepared SQL statement [SELECT * FROM comments
WHERE post_id  = ?
]
...
```





After applying the data loader.

```bash
2021-06-14 17:14:17.151 DEBUG 16832 --- [nio-8080-exec-1] o.s.jdbc.datasource.DataSourceUtils      : Fetching JDBC Connection from DataSource
2021-06-14 17:14:17.172 DEBUG 16832 --- [nio-8080-exec-1] o.s.jdbc.core.JdbcTemplate               : Executing prepared SQL query
2021-06-14 17:14:17.172 DEBUG 16832 --- [nio-8080-exec-1] o.s.jdbc.core.JdbcTemplate               : Executing prepared SQL statement [SELECT * FROM comments
WHERE post_id  in (?, ?, ?, ?)
]
```

