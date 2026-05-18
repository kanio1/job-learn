import { defineStore } from 'pinia'

export const useAppShellStore = defineStore('app-shell', {
  state: () => ({
    phaseLabel: 'Foundation',
    selectedArea: 'overview'
  }),
  actions: {
    selectArea(area: string) {
      this.selectedArea = area
    }
  }
})
