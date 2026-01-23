# SPDX-FileCopyrightText: 2023 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
# SPDX-License-Identifier: Apache-2.0

FROM eclipse-temurin:21-jre-alpine
COPY ./build/install/ /opt/
WORKDIR /opt/example-app
CMD ["sh", "./bin/example-app"]
EXPOSE 8080
