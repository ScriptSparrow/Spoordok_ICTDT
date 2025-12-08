#PostGis as default base
FROM postgis/postgis:15-3.5

# Install  pgvector
RUN apt-get update && apt-get install -y --no-install-recommends \
    postgresql-15-pgvector \
    && rm -rf /var/lib/apt/lists/*