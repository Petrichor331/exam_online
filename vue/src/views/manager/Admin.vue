<template>
  <div>
    <div class="card" style="margin-bottom: 5px">
      <el-input v-model="data.query" prefix-icon ="Search" placeholder="请输入用户名" style="width: 200px"></el-input>
      <el-button type="primary" @click="load">查询</el-button>
      <el-button type="warning" plain @click="reset">重置</el-button>
    </div>
    <div class="card" style="margin-bottom: 5px">
      <el-button type="primary" plain @click="handleAdd">新增</el-button>
      <el-button type="danger" plain @click="delBatch">批量删除</el-button>
    </div>

    <div class="card" style="margin-bottom: 5px">
      <el-table stripe :data="data.tableData" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="username" label="账号" />
        <el-table-column prop="avatar" label="头像" >
          <template v-slot="scope">
            <el-image style="width:50px; height:50px; border-radius: 50%; display: block "
                      :src="scope.row.avatar" :preview-src-list="[scope.row.avatar]" preview-teleported ></el-image>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="姓名" />
        <el-table-column prop="role" label="角色" />
        <el-table-column prop="phone" label="电话" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column label="操作" width="100" fixed="right">
          <template v-slot="scope">
            <el-button type="primary" circle :icon="Edit" @click="handleEdit(scope.row)"></el-button>
            <el-button type="danger" circle :icon="Delete" @click="handleDelete(scope.row.id)"></el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="card" v-if="data.total">
      <el-pagination
        v-model:current-page="data.pageNum"
        :page-size="data.pageSize"
        background layout="total, prev, pager, next, jumper"
        :total="data.total"
        @size-change="size => data.pageSize = size"
        @current-change="load"
        >
      </el-pagination>
    </div>

    <el-dialog title="管理员信息" v-model="data.formVisible" width="40%" destroy-on-close>
      <el-form ref="form" :model="data.form" label-width="70px" style="padding: 20px">
        <el-form-item prop="username" label="用户名">
          <el-input v-model="data.form.username" placeholder="请输入用户名"></el-input>
        </el-form-item>
        <el-form-item prop="avatar" label="头像">
          <el-upload
              :action="baseUrl + '/files/upload'"
              :on-success="handleFileUpload"
              list-type="picture"
              >
            <el-button type="primary">上传头像</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item prop="name" label="姓名">
          <el-input v-model="data.form.name" placeholder="请输入姓名"></el-input>
        </el-form-item>
        <el-form-item prop="phone" label="电话">
          <el-input v-model="data.form.phone" placeholder="请输入电话"></el-input>
        </el-form-item>
        <el-form-item prop="email" label="邮箱">
          <el-input v-model="data.form.email" placeholder="请输入邮箱"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
    <span class="dialog-footer">
      <el-button @click="data.formVisible = false">取 消</el-button>
      <el-button type="primary" @click="save">确 定</el-button>
    </span>
      </template>
    </el-dialog>
  </div>


</template>


<script setup>

import {reactive} from "vue";
import request from "@/utils/request.js";
import {ElMessage, ElMessageBox} from "element-plus";
import {Delete, Edit} from "@element-plus/icons-vue";

const baseUrl = import.meta.env.VITE_BASE_URL

const data = reactive({
  formVisible: false,
  form: {},
  tableData: [],
  pageNum: 1,
  pageSize: 10,
  total: 0,
  query: '',
  ids: [],
})

const load = () =>{
  request.get('/admin/selectPage', {
    params:{
      pageNum:data.pageNum,
      pageSize:data.pageSize,
      username:data.query
    }
  }).then(res => {
    if(res.code === '200'){
      data.tableData = res.data?.list || []
      data.total = res.data?.total
    }
  })
}

const handleFileUpload = (res) => {
  data.form.avatar = res.data
}

const handleAdd = () => {
  data.form = {}
  data.formVisible = true
}

const handleEdit = (row) => {
  data.form = JSON.parse(JSON.stringify(row))
  data.formVisible = true
}

const handleDelete = (id) => {
  ElMessageBox.confirm('删除后数据无法恢复，您确定删除吗？', '删除确认', { confirmButtonText:'确定', cancelButtonText:'取消', type:'warning'}).then(res =>{
    request.delete('/admin/delete/' + id).then(res => {
      if (res.code === '200') {
        ElMessage.success('操作成功')
        load()
      }else{
          ElMessage.error(res.msg)
      }
    })
  }).catch(err =>{
    console.error(err)
  })
}

const delBatch = () =>{
  if(!data.ids.length){
    ElMessage.warning('请选择要删除的数据')
    return
  }
  ElMessageBox.confirm('删除后数据无法恢复，您确定删除吗？', '删除确认', { confirmButtonText:'确定', cancelButtonText:'取消', type:'warning'}).then(res =>{
    request.delete('/admin/delete/Batch', {data:data.ids}).then(res => {
      if(res.code === '200'){
        ElMessage.success('操作成功')
        load()
      }else{
        ElMessage.error(res.msg)
      }
    })
  }).catch(err =>{
    console.error(err)
  })
}

const handleSelectionChange = (rows) => {
  data.ids = rows.map(v =>v.id)
  console.log(data.ids)
}


const add = () => {
  request.post('/admin/add', data.form).then(res => {
    if (res.code === '200') {
      ElMessage.success('操作成功')
      data.formVisible = false
      load()
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const update = () =>{
  request.put('/admin/update', data.form).then(res=>{
    if(res.code==='200'){
      ElMessage.success('操作成功')
      data.formVisible = false
      load()
    }
  })
}

const save = () => {
  data.form.id ? update() : add()
}

const reset = () => {
  data.query = null
      load()
}

load()

</script>

<style scoped>
.card {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 20px;
  margin-bottom: 10px;
}
.el-button--primary {
  background-color: #ffb7c5;
  border-color: #ffb7c5;
}

/* 改 plain 按钮的文字颜色 */
.el-button--primary.is-plain {
  color: #fff;

}
</style>