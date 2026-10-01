
FAUT_OPS_USER
FAUT_ADMIN_NPA

LSO_NPA

PURPOSE_USER (purpose)
PURPOSE_PIPELINE (purpose)

Solar groups:
  G_ADMIN_GROUP
  G_LSO_GROUP


# Auht
- LDAP
  - X-Credential-Identifier username
  - X-Authenticated-Groups  groups

    - G_ADMIN_GROUP in <groups>       --> FAUT_ADMIN_NPA
    - 'DSNLR-P22771-OPS' in groups    --> FAUT_OPS_USER
    - 'DSNLR-<purpose>-OPS' in groups --> PURPOSE_USER <purpose>
    - 'DSNLR-<purpose>-DEV' in groups --> PURPOSE_USER <purpose>

    - G_LSO_GROUPS in <groups>        --> LSO_NPA
  
