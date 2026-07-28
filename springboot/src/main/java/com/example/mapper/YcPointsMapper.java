package com.example.mapper;

import com.example.entity.YcPointsRechargeOrder;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface YcPointsMapper {

    @Insert("INSERT INTO yc_points_recharge_order " +
            "(recharge_no, user_id, amount, points_amount, status) " +
            "VALUES (#{rechargeNo}, #{userId}, #{amount}, #{pointsAmount}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertRechargeOrder(YcPointsRechargeOrder order);

    @Select("SELECT r.*, u.username, u.display_name, rv.display_name AS payment_review_by_name " +
            "FROM yc_points_recharge_order r " +
            "LEFT JOIN yc_user_account u ON u.id = r.user_id " +
            "LEFT JOIN yc_user_account rv ON rv.id = r.payment_review_by " +
            "WHERE r.id = #{id} LIMIT 1")
    YcPointsRechargeOrder selectRechargeOrderById(@Param("id") Long id);

    @Select({
            "<script>",
            "SELECT COUNT(1) FROM yc_points_recharge_order r ",
            "LEFT JOIN yc_user_account u ON u.id = r.user_id ",
            "WHERE 1=1 ",
            "<if test='status != null and status != \"\"'>AND r.status = #{status} </if>",
            "<if test='keyword != null and keyword != \"\"'>",
            "AND (r.recharge_no LIKE CONCAT('%', #{keyword}, '%') ",
            "OR u.username LIKE CONCAT('%', #{keyword}, '%') ",
            "OR u.display_name LIKE CONCAT('%', #{keyword}, '%')) ",
            "</if>",
            "</script>"
    })
    int countAdminRechargeOrders(@Param("keyword") String keyword, @Param("status") String status);

    @Select({
            "<script>",
            "SELECT r.*, u.username, u.display_name, rv.display_name AS payment_review_by_name ",
            "FROM yc_points_recharge_order r ",
            "LEFT JOIN yc_user_account u ON u.id = r.user_id ",
            "LEFT JOIN yc_user_account rv ON rv.id = r.payment_review_by ",
            "WHERE 1=1 ",
            "<if test='status != null and status != \"\"'>AND r.status = #{status} </if>",
            "<if test='keyword != null and keyword != \"\"'>",
            "AND (r.recharge_no LIKE CONCAT('%', #{keyword}, '%') ",
            "OR u.username LIKE CONCAT('%', #{keyword}, '%') ",
            "OR u.display_name LIKE CONCAT('%', #{keyword}, '%')) ",
            "</if>",
            "ORDER BY r.updated_at DESC, r.id DESC ",
            "LIMIT #{limit} OFFSET #{offset}",
            "</script>"
    })
    List<YcPointsRechargeOrder> selectAdminRechargeOrders(@Param("keyword") String keyword,
                                                          @Param("status") String status,
                                                          @Param("offset") int offset,
                                                          @Param("limit") int limit);

    @Select("SELECT r.*, u.username, u.display_name, rv.display_name AS payment_review_by_name " +
            "FROM yc_points_recharge_order r " +
            "LEFT JOIN yc_user_account u ON u.id = r.user_id " +
            "LEFT JOIN yc_user_account rv ON rv.id = r.payment_review_by " +
            "WHERE r.user_id = #{userId} " +
            "ORDER BY r.created_at DESC, r.id DESC LIMIT #{limit} OFFSET #{offset}")
    List<YcPointsRechargeOrder> selectUserRechargeOrders(@Param("userId") Long userId,
                                                         @Param("offset") int offset,
                                                         @Param("limit") int limit);

    @Update("UPDATE yc_points_recharge_order SET status = 'PENDING_REVIEW', payment_marked_at = NOW(), updated_at = NOW() " +
            "WHERE id = #{id} AND user_id = #{userId} AND status = 'PENDING_PAYMENT'")
    int markRechargePaid(@Param("id") Long id, @Param("userId") Long userId);

    @Update("UPDATE yc_points_recharge_order SET status = #{status}, payment_reviewed_at = NOW(), " +
            "payment_review_by = #{reviewBy}, payment_review_remark = #{remark}, updated_at = NOW() " +
            "WHERE id = #{id} AND status = 'PENDING_REVIEW'")
    int reviewRecharge(@Param("id") Long id,
                       @Param("status") String status,
                       @Param("reviewBy") Long reviewBy,
                       @Param("remark") String remark);

    @Update("UPDATE yc_user_account SET points_balance = points_balance + #{points}, " +
            "points_total_recharged = points_total_recharged + #{points}, updated_at = NOW() " +
            "WHERE id = #{userId}")
    int addUserPoints(@Param("userId") Long userId, @Param("points") int points);

    @Update("UPDATE yc_user_account SET points_balance = points_balance - #{points}, " +
            "points_total_spent = points_total_spent + #{points}, updated_at = NOW() " +
            "WHERE id = #{userId} AND points_is_unlimited = 0 AND points_balance >= #{points}")
    int spendUserPoints(@Param("userId") Long userId, @Param("points") int points);

    @Update("UPDATE yc_user_account SET points_balance = points_balance + #{points}, " +
            "points_total_spent = GREATEST(points_total_spent - #{points}, 0), updated_at = NOW() " +
            "WHERE id = #{userId} AND points_is_unlimited = 0")
    int refundUserPoints(@Param("userId") Long userId, @Param("points") int points);
}
