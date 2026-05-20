# metro-altroutes 发布清单

## 发布前

- [ ] `mvn test` 通过
- [ ] `mvn verify` 通过（含测试、覆盖率、SpotBugs）
- [ ] `mvn clean verify package` 生成 `target/metro-altroutes.jar`
- [ ] GitHub Actions CI workflow 在发布分支上为绿色
- [ ] 手工回归测试完成（见下方）
- [ ] `plugin.yml` 版本号和命令/权限描述准确
- [ ] `plugin.yml` 权限与 README 权限表一致
- [ ] CHANGELOG.md 已更新（中英文）
- [ ] README.md / README-en.md 命令表与实现一致
- [ ] 发布说明草稿已准备

## 运行时验证

- [ ] `/m line setstatus <id> normal|suspended|maintenance` 正常切换状态
- [ ] 暂停线路无法上车（`BoardingListener` 拦截）
- [ ] 暂停线路显示自定义公告
- [ ] `/m line setaltroute <id> <altId> [priority]` 添加替代路线
- [ ] `/m line clearaltroute <id> [altId]` 清除替代路线
- [ ] `/m line setautoresume <id> <minutes>` 自动恢复倒计时正常触发
- [ ] `/m line cancelautoresume <id>` 取消自动恢复
- [ ] `/m line setschedule <id> HH:mm-HH:mm` 计划维护时段自动切换
- [ ] `/m line clearschedule <id>` 取消计划维护
- [ ] `/m line stats <id>` 显示统计数据
- [ ] `/m line status <id>` 显示运营状态
- [ ] `/m line info <id>` 显示详细信息
- [ ] `/m line list` 列出全部线路
- [ ] `/m reload` 重新加载配置和缓存
- [ ] PlaceholderAPI 占位符正常工作（如已安装）
- [ ] `/m reload` 后所有数据保持正确
- [ ] 服务器日志无严重错误

## 打包

- [ ] 最终产物：`target/metro-altroutes.jar`
- [ ] CHANGELOG 包含行为变更
- [ ] 发布说明已从模板起草

## 发布说明模板

```markdown
## Added

- 

## Changed

- 

## Fixed

- 

## Migration Notes

- Config changes:
- Backup/rollback notes:

## Compatibility Notes

- Minecraft: 1.20.4+
- Server: Paper / Folia
- Dependencies: Metro 1.1.7+, PlaceholderAPI (optional)

## Verification

- `mvn verify`:
- Manual baseline:
```
