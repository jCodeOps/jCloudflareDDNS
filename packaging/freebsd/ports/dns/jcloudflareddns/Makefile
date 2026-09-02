PORTNAME=	jcloudflareddns
DISTVERSION=	0.1.1
CATEGORIES=	dns java
MASTER_SITES=	https://github.com/jCodeOps/jCloudflareDDNS/releases/download/v0.1.1/
DISTNAME=	${PORTNAME}-0.1.1-distribution

MAINTAINER=	jcabrerav@proactiveidea.com
COMMENT=	Secure, lightweight Cloudflare Dynamic DNS client
WWW=		https://github.com/jCodeOps/jCloudflareDDNS

LICENSE=	APACHE20
LICENSE_FILE=	${WRKSRC}/LICENSE

USES=		java:run
JAVA_VERSION=	25
USE_RC_SUBR=	jcloudflareddns

USERS=		jcloudflareddns
GROUPS=		jcloudflareddns

NO_ARCH=	yes
NO_BUILD=	yes

WRKSRC=		${WRKDIR}/${PORTNAME}-0.1.1
SUB_FILES=	jcloudflareddns-cli jcloudflareddns-run pkg-message
SUB_LIST=	APP_HOME=${JAVASHAREDIR}/${PORTNAME} \
		JAVA=${JAVA}

DOC_FILES=	CHANGELOG.md LICENSE README.md THIRD-PARTY-NOTICES.md \
		jcloudflareddns-sbom.json jcloudflareddns-sbom.xml

do-install:
	@${MKDIR} ${STAGEDIR}${JAVASHAREDIR}/${PORTNAME}/lib
	${INSTALL_DATA} ${WRKSRC}/lib/*.jar ${STAGEDIR}${JAVASHAREDIR}/${PORTNAME}/lib
	${INSTALL_SCRIPT} ${WRKDIR}/jcloudflareddns-cli ${STAGEDIR}${PREFIX}/bin/jcloudflareddns
	@${MKDIR} ${STAGEDIR}${PREFIX}/libexec
	${INSTALL_SCRIPT} ${WRKDIR}/jcloudflareddns-run ${STAGEDIR}${PREFIX}/libexec
	@${MKDIR} ${STAGEDIR}${ETCDIR}
	${INSTALL_DATA} ${FILESDIR}/config.yml.sample ${STAGEDIR}${ETCDIR}
	${INSTALL_DATA} ${FILESDIR}/tokens.env.sample ${STAGEDIR}${ETCDIR}
	${TOUCH} ${STAGEDIR}${ETCDIR}/config.yml.jcloudflareddns.lock
	@${MKDIR} ${STAGEDIR}${DOCSDIR}
	${INSTALL_DATA} ${DOC_FILES:S|^|${WRKSRC}/|} ${STAGEDIR}${DOCSDIR}

.include <bsd.port.mk>
