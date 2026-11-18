<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getDashboard } from '@/api/stats'
import type { Dashboard } from '@/api/stats'
import StatsChart from '@/components/StatsChart.vue'

const data = ref<Dashboard | null>(null)

onMounted(async () => {
  data.value = await getDashboard().catch(() => null)
})

const trendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 40, right: 20, top: 20, bottom: 30 },
  xAxis: { type: 'category', data: (data.value?.trend || []).map((t) => t.date) },
  yAxis: { type: 'value', minInterval: 1 },
  series: [{ name: '借阅量', type: 'line', smooth: true, data: (data.value?.trend || []).map((t) => t.count), areaStyle: {} }],
}))

const categoryOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: { orient: 'vertical', left: 'left' },
  series: [
    {
      name: '分类占比',
      type: 'pie',
      radius: '60%',
      data: (data.value?.categoryStats || []).map((c) => ({ name: c.name, value: c.value })),
    },
  ],
}))

const hotOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 140, right: 30, top: 20, bottom: 30 },
  xAxis: { type: 'value', minInterval: 1 },
  yAxis: { type: 'category', data: (data.value?.hotBooks || []).map((b) => b.title).reverse() },
  series: [{ name: '借阅次数', type: 'bar', data: (data.value?.hotBooks || []).map((b) => b.borrowCount).reverse() }],
}))
</script>

<template>
  <div>
    <!-- 指标卡 -->
    <el-row :gutter="16">
      <el-col v-for="card in [
        { title: '用户总数', value: data?.userCount, icon: 'User', color: '#409eff' },
        { title: '图书总数', value: data?.bookCount, icon: 'Reading', color: '#67c23a' },
        { title: '借出中', value: data?.borrowingCount, icon: 'Notebook', color: '#e6a23c' },
        { title: '逾期数', value: data?.overdueCount, icon: 'Warning', color: '#f56c6c' },
      ]" :key="card.title" :sm="12" :md="6" style="margin-bottom: 16px">
        <el-card shadow="hover">
          <div class="metric">
            <el-icon :size="40" :color="card.color">
              <component :is="card.icon" />
            </el-icon>
            <div>
              <div class="metric-value">{{ card.value ?? '-' }}</div>
              <div class="text-muted">{{ card.title }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表 -->
    <el-row :gutter="16">
      <el-col :md="14" style="margin-bottom: 16px">
        <el-card shadow="never">
          <template #header>近 30 天借阅趋势</template>
          <StatsChart :option="trendOption" />
        </el-card>
      </el-col>
      <el-col :md="10" style="margin-bottom: 16px">
        <el-card shadow="never">
          <template #header>分类借阅占比</template>
          <StatsChart :option="categoryOption" />
        </el-card>
      </el-col>
      <el-col :span="24">
        <el-card shadow="never">
          <template #header>热门图书 Top10</template>
          <StatsChart :option="hotOption" height="380px" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.metric {
  display: flex;
  align-items: center;
  gap: 16px;
}
.metric-value {
  font-size: 26px;
  font-weight: 700;
}
</style>
