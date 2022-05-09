package cn.agent.infrastructure.mysql2postgresql;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * test SQL
 */
@SpringBootTest(classes = ApplicationTests.class)
@Rollback
@Transactional
@EnabledIfDbAvailable
public class SqlTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void testAddDate() {
        String sql = "SELECT t.id FROM xxl_job_registry AS t WHERE t.update_time < DATE_ADD(?,INTERVAL - ? SECOND)";
        List<Long> longs = jdbcTemplate.queryForList(sql, Long.class, new Date(), 10);
        Assertions.assertNotNull(longs);
    }

    @Test
    void testUpdateQuote() {
        String sql = "UPDATE xxl_job_group   SET `app_name` = ?,    `title` = ?,    `address_type` = ?,    `address_list` = ?,    `update_time` = ?   WHERE id = ?";
        int update = jdbcTemplate.update(sql, "a", "b", "c", "d", new Date(), 1);
        System.out.println(update);
    }

    @Test
    void testInsertLog() {
        String sql = "INSERT INTO xxl_job_log (job_group, job_id, trigger_time, trigger_code, handle_code) VALUES (1, 1, '2022-05-05 21:41:55.074000', 0, 0)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int update = jdbcTemplate.update(connection -> connection.prepareStatement(sql, new String[]{"id"}), keyHolder);
        long id = keyHolder.getKey().longValue();
        System.out.println(id);
    }

    @Test
    void testLimit() {
        String sql = "SELECT t.id, t.job_group, t.job_desc, t.add_time, t.update_time , t.author, t.alarm_email, t.schedule_type, t.schedule_conf, t.misfire_strategy , t.executor_route_strategy, t.executor_handler, t.executor_param, t.executor_block_strategy, t.executor_timeout , t.executor_fail_retry_count, t.glue_type, t.glue_source, t.glue_remark, t.glue_updatetime , t.child_jobid, t.trigger_status, t.trigger_last_time, t.trigger_next_time FROM xxl_job_info t WHERE t.job_group = ? ORDER BY id DESC LIMIT ?, ?";
        List<Map<String, Object>> query = jdbcTemplate.queryForList(sql, 1, 0, 10);
        System.out.println(query);
    }

    @Test
    void testSubLimit() {
        String sql = "SELECT id FROM xxl_job_log WHERE id NOT IN ( SELECT id FROM ( SELECT id FROM xxl_job_log t ORDER BY t.trigger_time DESC LIMIT 0, ? ) t1 ) ORDER BY id ASC LIMIT ?";
        List<Long> longs = jdbcTemplate.queryForList(sql, Long.class, 1000, 1000);
        System.out.println(longs);
    }

    @Test
    void testDeleteLimit() {
        String sql = "DELETE FROM his_config_info WHERE gmt_modified < ? LIMIT ?";
        int longs = jdbcTemplate.update(sql, 1000, 1000);
        System.out.println(longs);
    }

    @Test
    void testDeleteSubLimit() {
        String sql = "DELETE FROM xxl_job_logglue WHERE id NOT IN ( SELECT id FROM ( SELECT id FROM xxl_job_logglue WHERE `job_id` = ? ORDER BY update_time DESC LIMIT 0, ? ) t1 ) AND `job_id` = ?";
        int longs = jdbcTemplate.update(sql, 2, 2, 30);
        System.out.println(longs);
    }
}
