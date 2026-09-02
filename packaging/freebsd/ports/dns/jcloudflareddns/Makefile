PORTNAME=	jcloudflareddns
DISTVERSION=	0.1.0
CATEGORIES=	dns java
MASTER_SITES=	https://github.com/jCodeOps/jCloudflareDDNS/releases/download/v0.1.0/
DISTNAME=	${PORTNAME}-0.1.0-distribution

MAINTAINER=	jcabrerav@proactiveidea.com
COMMENT=	Secure, lightweight Cloudflare Dynamic DNS client
WWW=		https://github.com/jCodeOps/jCloudflareDDNS

LICENSE=	APACHE20
LICENSE_FILE=	${WRKSRC}/LICENSE

USES=		java:run
JAVA_VERSION=	25

NO_ARCH=	yes
NO_BUILD=	yes

WRKSRC=		${WRKDIR}/${PORTNAME}-0.1.0
SUB_FILES=	jcloudflareddns
SUB_LIST=	APP_HOME=${JAVASHAREDIR}/${PORTNAME} \
		JAVA=${JAVA}

DOC_FILES=	CHANGELOG.md LICENSE README.md THIRD-PARTY-NOTICES.md \
		jcloudflareddns-sbom.json jcloudflareddns-sbom.xml

do-install:
	@${MKDIR} ${STAGEDIR}${JAVASHAREDIR}/${PORTNAME}/lib
	${INSTALL_DATA} ${WRKSRC}/lib/*.jar ${STAGEDIR}${JAVASHAREDIR}/${PORTNAME}/lib
	${INSTALL_SCRIPT} ${WRKDIR}/jcloudflareddns ${STAGEDIR}${PREFIX}/bin
	@${MKDIR} ${STAGEDIR}${DOCSDIR}
	${INSTALL_DATA} ${DOC_FILES:S|^|${WRKSRC}/|} ${STAGEDIR}${DOCSDIR}

.include <bsd.port.mk>
