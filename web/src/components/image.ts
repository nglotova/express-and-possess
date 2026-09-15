const MAX_SIDE = 1600;
const QUALITY = 0.85;

/**
 * Shrinks a photo before upload. Phone cameras produce 4000-pixel, 5 MB pictures, and
 * iPhones produce HEIC files the server does not accept; drawing the picture onto a canvas
 * and exporting JPEG fixes both. Anything that goes wrong falls back to the original file.
 */
export async function prepareImage(file: File): Promise<File> {
  if (!file.type.startsWith("image/") && !/\.(heic|heif)$/i.test(file.name)) {
    return file;
  }
  try {
    const bitmap = await createImageBitmap(file);
    const scale = Math.min(1, MAX_SIDE / Math.max(bitmap.width, bitmap.height));
    const width = Math.round(bitmap.width * scale);
    const height = Math.round(bitmap.height * scale);
    const canvas = document.createElement("canvas");
    canvas.width = width;
    canvas.height = height;
    const context = canvas.getContext("2d");
    if (!context) return file;
    context.drawImage(bitmap, 0, 0, width, height);
    bitmap.close();
    const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, "image/jpeg", QUALITY));
    if (!blob) return file;
    return new File([blob], "picture.jpg", { type: "image/jpeg" });
  } catch {
    return file;
  }
}
