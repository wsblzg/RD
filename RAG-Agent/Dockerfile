FROM python:3.11-slim

WORKDIR /app
ENV TZ=Asia/Shanghai \
    HOST=0.0.0.0 \
    PORT=17690

COPY --from=ghcr.io/astral-sh/uv:latest /uv /uvx /bin/
COPY pyproject.toml uv.lock ./
RUN uv sync --frozen --no-dev

COPY main.py ./
COPY templates/ templates/
COPY static/ static/
COPY docs/ docs/

RUN mkdir -p logs models vector_db

EXPOSE 17690
CMD ["uv", "run", "--frozen", "--no-dev", "python", "main.py"]
