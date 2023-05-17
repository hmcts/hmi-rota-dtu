 # renovate: datasource=github-releases depName=microsoft/ApplicationInsights-Java
ARG APP_INSIGHTS_AGENT_VERSION=3.4.10
FROM hmctspublic.azurecr.io/base/java:17-distroless

COPY build/libs/hmi-rota-dtu.jar /opt/app/
COPY lib/applicationinsights.json /opt/app/

EXPOSE 3456
CMD [ "hmi-rota-dtu.jar"]
