#!/bin/bash

# Generating updated sitemap for online documentation.

CURRENT_DATE=`date +%Y-%m-%d`

if [ ! -d sitemap ]; then
  mkdir sitemap;
fi

cat > sitemap/sitemap.xml << EOM
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.sitemaps.org/schemas/sitemap/0.9 http://www.sitemaps.org/schemas/sitemap/0.9/sitemap.xsd">
    <url>
        <loc>http://docs.inspectit.rocks/</loc>
        <changefreq>weekly</changefreq>
        <lastmod>$CURRENT_DATE</lastmod>
    </url>
    <url>
        <loc>http://docs.inspectit.rocks/releases/latest/</loc>
        <changefreq>weekly</changefreq>
        <lastmod>$CURRENT_DATE</lastmod>
    </url>
    <url>
        <loc>http://docs.inspectit.rocks/releases/</loc>
        <changefreq>weekly</changefreq>
        <lastmod>$CURRENT_DATE</lastmod>
    </url>
    <url>
        <loc>http://docs.inspectit.rocks/master</loc>
        <changefreq>weekly</changefreq>
    </url>
EOM

for tag in `git tag`
do
cat >> sitemap/sitemap.xml << EOM
    <url>
        <loc>http://docs.inspectit.rocks/releases/$tag</loc>
    </url>
EOM
done

echo "</urlset>" >> sitemap/sitemap.xml
