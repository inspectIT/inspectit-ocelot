while IFS='=' read -r ID FILE || [ -n "$FILE" ]
do
  DASHBOARD_JSON=$(cat "$FILE") || exit 1
  curl -f -s -S -F json="${DASHBOARD_JSON}" -H "Authorization: Bearer $GRAFANA_API_KEY" https://grafana.com/api/dashboards/$ID/revisions > /dev/null
done < "dashboard-mapping.txt"