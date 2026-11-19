# Firma del APK

Android solo instala una versión nueva encima de la anterior si las dos están firmadas
con la **misma clave**. La clave no se guarda en el repositorio (es público): GitHub Actions
la toma del secreto `FIRMA_KEYSTORE_B64`.

- `HUELLA.txt` tiene la huella SHA-256 del certificado esperado. La compilación la verifica
  y, si no coincide o falta el secreto, **no publica la Release**.
- Para cargar el secreto: *Settings → Secrets and variables → Actions → New repository secret*,
  nombre `FIRMA_KEYSTORE_B64`, valor: el contenido del archivo `FIRMA_KEYSTORE_B64.txt`
  (el archivo de clave en base64). Guardá una copia de ese archivo en un lugar seguro:
  sin esa clave no se pueden publicar actualizaciones que se instalen encima.
