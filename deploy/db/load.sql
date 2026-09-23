CREATE DATABASE ebms;
GRANT ALL ON ebms.* to 'corvus'@'%' IDENTIFIED BY 'corvus';
USE ebms;
SOURCE /build/ebms.sql

CREATE DATABASE as2;
GRANT ALL ON as2.* to 'corvus'@'%' IDENTIFIED BY 'corvus';
USE as2;
SOURCE /build/as2.sql

CREATE DATABASE as2plus;
GRANT ALL ON as2plus.* to 'corvus'@'%' IDENTIFIED BY 'corvus';
USE as2plus;
SOURCE /build/as2plus.sql

CREATE DATABASE sfrm;
GRANT ALL ON sfrm.* to 'corvus'@'%' IDENTIFIED BY 'corvus';
USE sfrm;
SOURCE /build/sfrm.sql

CREATE DATABASE apikeys;
GRANT ALL ON apikeys.* to 'corvus'@'%' IDENTIFIED BY 'corvus';
USE apikeys;
SOURCE /build/apikeys.sql
