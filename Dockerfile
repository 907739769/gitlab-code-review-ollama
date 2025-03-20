FROM eclipse-temurin:8u412-b08-jre-jammy
LABEL title="gitlab-code-review-ollama"
LABEL description="基于ollama大模型的GitLab代码审查"
LABEL authors="JackDing"
RUN apt-get update && apt-get install -y gosu && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY ./target/application.jar /app/gitlab-code-review-ollama.jar
COPY --chmod=755 entrypoint.sh /entrypoint.sh
ENV TZ=Asia/Shanghai
ENV apiGitlabUrl=""
ENV apiGitlabToken=""
ENV apiOllamaUrl=""
ENV apiOllamaModel=""
ENV apiOllamaSystem=""
ENV apiOllamaPrompt=""
ENV apiOllamaOptions=""
ENV PUID=0
ENV PGID=0
ENV UMASK=022
ENV JAVA_OPTS="-Xms32m -Xmx512m"
ENTRYPOINT [ "/entrypoint.sh" ]
VOLUME /log