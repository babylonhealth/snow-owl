# Runbook

## Running Locally

You'll need secrets from [Vault] to allow the service to connect to its dependencies and configuration. 
Set these environment variables.

SnowOwl is a third-party dependency used by the service clinical-terminology.
The official documentation to run SnowOwl by b2i is: [SnowOwl b2i](https://docs.b2i.sg/snow-owl)
 
The list of credentials in vault is configured in the manifest. It is currently:
 - SNOWOWL_USERNAME
 - SNOWOWL_PASSWORD
The file users.j2 is mounted to specify credentials to use for accessing SnowOwl.

SnowOwl uses Elasticsearch clusters as dependency.

The main configuration file is the following: `custom_docker/snowowl.yml`. 

## Dependencies and Clients
 
 Clinical-Terminology is the only user of SnowOwl.

## Deploying

- **dev-internal** deploys on every commit to master.
- **staging-internal** deploys on every [tagged release].
- **prod-internal*** deploys when the [manifests] are changed.

## Monitoring

To see the service health and versions, look at [Kompass]. 
You can also do the same manually by contacting [/health].

For logs, head to [logz.io] and search for `application:snowowl`.
 
## Alerting

## Troubleshooting

Contact [#chr_support] if you have any issues with the service.

One of the easiest, safest thing to do when there's an issue is restarting the pods. 

The [monitoring](#monitoring) and [alerting](#alerting) sections will help you gather information on any failures.

[vault]: https://engineering.ops.babylontech.co.uk/docs/cicd-vault-secrets/
[tagged release]: https://github.com/babylonhealth/snow-owl/releases
[tagged release]: https://github.com/b2ihealthcare/snow-owl/tags
[manifests]: https://github.com/babylonhealth/manifests/tree/master/services/snowowl
[kompass]: https://kompass.ops.babylontech.co.uk/?clusters=dev-internal
[#chr-alerts]: https://babylonhealth.slack.com/messages/chr-alerts/
[#chr-notifications]: https://babylonhealth.slack.com/messages/chr-notifications/
[#chr_support]: https://babylonhealth.slack.com/messages/chr_support/
