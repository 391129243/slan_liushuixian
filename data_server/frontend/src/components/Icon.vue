<script setup lang="ts">
defineProps<{ name: string; size?: number }>()
const paths: Record<string, string> = {
  grid: 'M3 3h7v7H3z M14 3h7v7h-7z M3 14h7v7H3z M14 14h7v7h-7z',
  folder: 'M3 6h6l2 2h10v12H3z M3 6V4h6l2 2',
  chart: 'M4 4v16h16 M7 15l4-7 4 4 5-7',
  clock: 'M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18 M12 7v5l3 2',
  settings: 'M4 6h16 M4 12h16 M4 18h16 M8 3v6 M16 9v6 M10 15v6',
  file: 'M14 3H5v18h14V8z M14 3v5h5 M8 12h8 M8 16h5',
  upload: 'M12 16V3 M7 8l5-5 5 5 M4 15v6h16v-6',
  download: 'M12 3v13 M7 11l5 5 5-5 M4 17v4h16v-4',
  play: 'M8 4l12 8-12 8z',
  arrow: 'M5 12h14 M14 7l5 5-5 5',
  check: 'M5 12l4 4L19 6',
  search: 'M10 3a7 7 0 1 0 0 14 7 7 0 0 0 0-14 M15 15l6 6',
  close: 'M6 6l12 12 M6 18L18 6',
  info: 'M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18 M12 11v6 M12 7h.01',
  chevron: 'M9 5l7 7-7 7',
  reset: 'M4 10a8 8 0 1 1 1 8 M4 4v6h6',
  save: 'M4 3h13l4 4v14H3V3z M7 3v6h9V3 M7 21v-8h10v8'
}
</script>
<template><svg :width="size || 20" :height="size || 20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.65" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path :d="paths[name] || paths.file" /></svg></template>
