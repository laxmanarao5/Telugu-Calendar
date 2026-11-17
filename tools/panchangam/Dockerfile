# Python + Swiss Ephemeris for the panchangam generator. Nothing is installed on the host.
# pyswisseph has no wheel for Linux, so it is compiled once here (needs g++).
FROM python:3.12-slim
RUN apt-get update \
 && apt-get install -y --no-install-recommends g++ libc6-dev \
 && pip install --no-cache-dir pyswisseph==2.10.3.2 \
 && apt-get purge -y g++ && apt-get autoremove -y \
 && rm -rf /var/lib/apt/lists/*
WORKDIR /work
