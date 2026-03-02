package com.daifu.manage.stats.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.daifu.manage.stats.entity.UserStatsDaily;
import com.daifu.manage.stats.vo.StatsSummaryVO;
import com.daifu.manage.stats.vo.UserStatsVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserStatsMapper extends BaseMapper<UserStatsDaily> {

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM sys_user u
            WHERE u.role = 2
            </script>
            """)
    long countUsersForStats();

    @Select("""
            <script>
            SELECT
                u.id AS userId,
                u.username AS username,
                COALESCE(SUM(c.quantity), 0) AS totalQuantity,
                COALESCE(SUM(c.total_amount), 0) AS totalAmount
            FROM sys_user u
            LEFT JOIN cart_item c ON c.user_id = u.id
            <if test="startTime != null">
                AND c.create_time <![CDATA[>=]]> #{startTime}
            </if>
            <if test="endTime != null">
                AND c.create_time <![CDATA[<=]]> #{endTime}
            </if>
            WHERE u.role = 2
            GROUP BY u.id, u.username
            ORDER BY u.id DESC
            LIMIT #{offset}, #{pageSize}
            </script>
            """)
    List<UserStatsVO> listUsersForStats(@Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime,
                                        @Param("offset") long offset,
                                        @Param("pageSize") int pageSize);

    @Select("""
            <script>
            SELECT
                COALESCE(COUNT(DISTINCT c.user_id), 0) AS activeUserCount,
                COALESCE(SUM(c.quantity), 0) AS totalQuantity,
                COALESCE(SUM(c.total_amount), 0) AS totalAmount
            FROM cart_item c
            INNER JOIN sys_user u ON u.id = c.user_id AND u.role = 2
            WHERE 1 = 1
            <if test="startTime != null">
                AND c.create_time <![CDATA[>=]]> #{startTime}
            </if>
            <if test="endTime != null">
                AND c.create_time <![CDATA[<=]]> #{endTime}
            </if>
            </script>
            """)
    StatsSummaryVO summary(@Param("startTime") LocalDateTime startTime,
                           @Param("endTime") LocalDateTime endTime);

    @Delete("DELETE FROM user_stats_daily WHERE stat_date = #{statDate}")
    int deleteDailyByDate(@Param("statDate") LocalDate statDate);

    @Insert("""
            INSERT INTO user_stats_daily (user_id, stat_date, cart_item_count, total_amount)
            SELECT
                u.id AS user_id,
                #{statDate} AS stat_date,
                COALESCE(SUM(c.quantity), 0) AS cart_item_count,
                COALESCE(SUM(c.total_amount), 0) AS total_amount
            FROM sys_user u
            LEFT JOIN cart_item c ON c.user_id = u.id
                AND c.create_time >= #{startTime}
                AND c.create_time < #{endTime}
            WHERE u.role = 2
            GROUP BY u.id
            """)
    int insertDailyByDate(@Param("statDate") LocalDate statDate,
                          @Param("startTime") LocalDateTime startTime,
                          @Param("endTime") LocalDateTime endTime);

    @Select("SELECT COUNT(1) FROM user_stats_daily WHERE stat_date = #{statDate}")
    int countDailyRows(@Param("statDate") LocalDate statDate);
}
