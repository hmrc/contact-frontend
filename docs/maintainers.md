# Maintaining contact-frontend and hmrc-deskpro

## ADRs in contact-frontend

We are using MADRs to record architecturally significant decisions in this service. To find out more
visit [MADR](https://github.com/adr/madr)

See our [architectural decision log](adr/index.md) (ADL) for a list of past decisions.

## How to create a new ADR

1. Install [Node](https://nodejs.org/en/download/) if you do not have this already. Node includes
npm.

1. Install `adr-log` if you do not have this already

    ```shell script
    npm install -g adr-log
    ```

1. Copy [template.md](adr/template.md) as NNNN-title-of-decision.md, and fill
in the fields. Do not feel you have to fill in all the fields, only fill in fields
that are strictly necessary. Some decisions will merit more detail than others.

1. To re-generate the table of contents, run

    ```shell script
    ./generate-adl.sh
    ```
