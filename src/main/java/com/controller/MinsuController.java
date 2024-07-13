
package com.controller;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.text.SimpleDateFormat;
import com.alibaba.fastjson.JSONObject;
import java.util.*;
import org.springframework.beans.BeanUtils;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.context.ContextLoader;
import javax.servlet.ServletContext;
import com.service.TokenService;
import com.utils.*;
import java.lang.reflect.InvocationTargetException;

import com.service.DictionaryService;
import org.apache.commons.lang3.StringUtils;
import com.annotation.IgnoreAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.entity.*;
import com.entity.view.*;
import com.service.*;
import com.utils.PageUtils;
import com.utils.R;
import com.alibaba.fastjson.*;

/**
 * 民宿信息
 * 后端接口
 * @author
 * @email
*/
@RestController
@Controller
@RequestMapping("/minsu")
public class MinsuController {
    private static final Logger logger = LoggerFactory.getLogger(MinsuController.class);

    private static final String TABLE_NAME = "minsu";

    @Autowired
    private MinsuService minsuService;


    @Autowired
    private TokenService tokenService;
    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private MinsuOrderService minsuOrderService;
    @Autowired
    private MinsuCollectionService minsuCollectionService;
    //级联表非注册的service
    //注册表service
    @Autowired
    private YonghuService yonghuService;
    @Autowired
    private ShangjiaService shangjiaService;


    /**
    * 后端列表
    */
    @RequestMapping("/page")
    public R page(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("page方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));
        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永不会进入");
        else if("用户".equals(role))
            params.put("yonghuId",request.getSession().getAttribute("userId"));
        else if("商家".equals(role))
            params.put("shangjiaId",request.getSession().getAttribute("userId"));
        params.put("minsuDeleteStart",1);params.put("minsuDeleteEnd",1);
        CommonUtil.checkMap(params);
        PageUtils page = minsuService.queryPage(params);

        //字典表数据转换
        List<MinsuView> list =(List<MinsuView>)page.getList();
        for(MinsuView c:list){
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(c, request);
        }
        return R.ok().put("data", page);
    }

    /**
    * 后端详情
    */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id, HttpServletRequest request){
        logger.debug("info方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        MinsuEntity minsu = minsuService.selectById(id);
        if(minsu !=null){
            //entity转view
            MinsuView view = new MinsuView();
            BeanUtils.copyProperties( minsu , view );//把实体数据重构到view中
            //级联表 商家
            //级联表
            ShangjiaEntity shangjia = shangjiaService.selectById(minsu.getShangjiaId());
            if(shangjia != null){
            BeanUtils.copyProperties( shangjia , view ,new String[]{ "id", "createTime", "insertTime", "updateTime", "shangjiaId"});//把级联的数据添加到view中,并排除id和创建时间字段,当前表的级联注册表
            view.setShangjiaId(shangjia.getId());
            }
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(view, request);
            return R.ok().put("data", view);
        }else {
            return R.error(511,"查不到数据");
        }

    }

    /**
    * 后端保存
    */
    @RequestMapping("/save")
    public R save(@RequestBody MinsuEntity minsu, HttpServletRequest request){
        logger.debug("save方法:,,Controller:{},,minsu:{}",this.getClass().getName(),minsu.toString());

        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永远不会进入");
        else if("商家".equals(role))
            minsu.setShangjiaId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));

        Wrapper<MinsuEntity> queryWrapper = new EntityWrapper<MinsuEntity>()
            .eq("shangjia_id", minsu.getShangjiaId())
            .eq("minsu_name", minsu.getMinsuName())
            .eq("minsu_types", minsu.getMinsuTypes())
            .eq("minsu_clicknum", minsu.getMinsuClicknum())
            .eq("shangxia_types", minsu.getShangxiaTypes())
            .eq("minsu_delete", minsu.getMinsuDelete())
            ;

        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        MinsuEntity minsuEntity = minsuService.selectOne(queryWrapper);
        if(minsuEntity==null){
            minsu.setMinsuClicknum(1);
            minsu.setShangxiaTypes(1);
            minsu.setMinsuDelete(1);
            minsu.setCreateTime(new Date());
            minsuService.insert(minsu);
            return R.ok();
        }else {
            return R.error(511,"表中有相同数据");
        }
    }

    /**
    * 后端修改
    */
    @RequestMapping("/update")
    public R update(@RequestBody MinsuEntity minsu, HttpServletRequest request) throws NoSuchFieldException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        logger.debug("update方法:,,Controller:{},,minsu:{}",this.getClass().getName(),minsu.toString());
        MinsuEntity oldMinsuEntity = minsuService.selectById(minsu.getId());//查询原先数据

        String role = String.valueOf(request.getSession().getAttribute("role"));
//        if(false)
//            return R.error(511,"永远不会进入");
//        else if("商家".equals(role))
//            minsu.setShangjiaId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));
        if("".equals(minsu.getMinsuPhoto()) || "null".equals(minsu.getMinsuPhoto())){
                minsu.setMinsuPhoto(null);
        }

            minsuService.updateById(minsu);//根据id更新
            return R.ok();
    }



    /**
    * 删除
    */
    @RequestMapping("/delete")
    public R delete(@RequestBody Integer[] ids, HttpServletRequest request){
        logger.debug("delete:,,Controller:{},,ids:{}",this.getClass().getName(),ids.toString());
        List<MinsuEntity> oldMinsuList =minsuService.selectBatchIds(Arrays.asList(ids));//要删除的数据
        ArrayList<MinsuEntity> list = new ArrayList<>();
        for(Integer id:ids){
            MinsuEntity minsuEntity = new MinsuEntity();
            minsuEntity.setId(id);
            minsuEntity.setMinsuDelete(2);
            list.add(minsuEntity);
        }
        if(list != null && list.size() >0){
            minsuService.updateBatchById(list);
        }

        return R.ok();
    }


    /**
     * 批量上传
     */
    @RequestMapping("/batchInsert")
    public R save( String fileName, HttpServletRequest request){
        logger.debug("batchInsert方法:,,Controller:{},,fileName:{}",this.getClass().getName(),fileName);
        Integer yonghuId = Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId")));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            List<MinsuEntity> minsuList = new ArrayList<>();//上传的东西
            Map<String, List<String>> seachFields= new HashMap<>();//要查询的字段
            Date date = new Date();
            int lastIndexOf = fileName.lastIndexOf(".");
            if(lastIndexOf == -1){
                return R.error(511,"该文件没有后缀");
            }else{
                String suffix = fileName.substring(lastIndexOf);
                if(!".xls".equals(suffix)){
                    return R.error(511,"只支持后缀为xls的excel文件");
                }else{
                    URL resource = this.getClass().getClassLoader().getResource("static/upload/" + fileName);//获取文件路径
                    File file = new File(resource.getFile());
                    if(!file.exists()){
                        return R.error(511,"找不到上传文件，请联系管理员");
                    }else{
                        List<List<String>> dataList = PoiUtil.poiImport(file.getPath());//读取xls文件
                        dataList.remove(0);//删除第一行，因为第一行是提示
                        for(List<String> data:dataList){
                            //循环
                            MinsuEntity minsuEntity = new MinsuEntity();
//                            minsuEntity.setShangjiaId(Integer.valueOf(data.get(0)));   //商家 要改的
//                            minsuEntity.setMinsuName(data.get(0));                    //房间名称 要改的
//                            minsuEntity.setMinsuPhoto("");//详情和图片
//                            minsuEntity.setMinsuTypes(Integer.valueOf(data.get(0)));   //房间类型 要改的
//                            minsuEntity.setMinsuNewMoney(data.get(0));                    //价格 要改的
//                            minsuEntity.setMinsuClicknum(Integer.valueOf(data.get(0)));   //点击次数 要改的
//                            minsuEntity.setShangxiaTypes(Integer.valueOf(data.get(0)));   //是否上架 要改的
//                            minsuEntity.setMinsuDelete(1);//逻辑删除字段
//                            minsuEntity.setMinsuContent("");//详情和图片
//                            minsuEntity.setCreateTime(date);//时间
                            minsuList.add(minsuEntity);


                            //把要查询是否重复的字段放入map中
                        }

                        //查询是否重复
                        minsuService.insertBatch(minsuList);
                        return R.ok();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            return R.error(511,"批量插入数据异常，请联系管理员");
        }
    }



    /**
    * 个性推荐
    */
    @IgnoreAuth
    @RequestMapping("/gexingtuijian")
    public R gexingtuijian(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("gexingtuijian方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));
        CommonUtil.checkMap(params);
        List<MinsuView> returnMinsuViewList = new ArrayList<>();

        //查询订单
        Map<String, Object> params1 = new HashMap<>(params);params1.put("sort","id");params1.put("yonghuId",request.getSession().getAttribute("userId"));
        PageUtils pageUtils = minsuOrderService.queryPage(params1);
        List<MinsuOrderView> orderViewsList =(List<MinsuOrderView>)pageUtils.getList();
        Map<Integer,Integer> typeMap=new HashMap<>();//购买的类型list
        for(MinsuOrderView orderView:orderViewsList){
            Integer minsuTypes = orderView.getMinsuTypes();
            if(typeMap.containsKey(minsuTypes)){
                typeMap.put(minsuTypes,typeMap.get(minsuTypes)+1);
            }else{
                typeMap.put(minsuTypes,1);
            }
        }
        List<Integer> typeList = new ArrayList<>();//排序后的有序的类型 按最多到最少
        typeMap.entrySet().stream().sorted((o1, o2) -> o2.getValue() - o1.getValue()).forEach(e -> typeList.add(e.getKey()));//排序
        Integer limit = Integer.valueOf(String.valueOf(params.get("limit")));
        for(Integer type:typeList){
            Map<String, Object> params2 = new HashMap<>(params);params2.put("minsuTypes",type);
            PageUtils pageUtils1 = minsuService.queryPage(params2);
            List<MinsuView> minsuViewList =(List<MinsuView>)pageUtils1.getList();
            returnMinsuViewList.addAll(minsuViewList);
            if(returnMinsuViewList.size()>= limit) break;//返回的推荐数量大于要的数量 跳出循环
        }
        //正常查询出来商品,用于补全推荐缺少的数据
        PageUtils page = minsuService.queryPage(params);
        if(returnMinsuViewList.size()<limit){//返回数量还是小于要求数量
            int toAddNum = limit - returnMinsuViewList.size();//要添加的数量
            List<MinsuView> minsuViewList =(List<MinsuView>)page.getList();
            for(MinsuView minsuView:minsuViewList){
                Boolean addFlag = true;
                for(MinsuView returnMinsuView:returnMinsuViewList){
                    if(returnMinsuView.getId().intValue() ==minsuView.getId().intValue()) addFlag=false;//返回的数据中已存在此商品
                }
                if(addFlag){
                    toAddNum=toAddNum-1;
                    returnMinsuViewList.add(minsuView);
                    if(toAddNum==0) break;//够数量了
                }
            }
        }else {
            returnMinsuViewList = returnMinsuViewList.subList(0, limit);
        }

        for(MinsuView c:returnMinsuViewList)
            dictionaryService.dictionaryConvert(c, request);
        page.setList(returnMinsuViewList);
        return R.ok().put("data", page);
    }

    /**
    * 前端列表
    */
    @IgnoreAuth
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("list方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));

        CommonUtil.checkMap(params);
        PageUtils page = minsuService.queryPage(params);

        //字典表数据转换
        List<MinsuView> list =(List<MinsuView>)page.getList();
        for(MinsuView c:list)
            dictionaryService.dictionaryConvert(c, request); //修改对应字典表字段

        return R.ok().put("data", page);
    }

    /**
    * 前端详情
    */
    @RequestMapping("/detail/{id}")
    public R detail(@PathVariable("id") Long id, HttpServletRequest request){
        logger.debug("detail方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        MinsuEntity minsu = minsuService.selectById(id);
            if(minsu !=null){

                //点击数量加1
                minsu.setMinsuClicknum(minsu.getMinsuClicknum()+1);
                minsuService.updateById(minsu);

                //entity转view
                MinsuView view = new MinsuView();
                BeanUtils.copyProperties( minsu , view );//把实体数据重构到view中

                //级联表
                    ShangjiaEntity shangjia = shangjiaService.selectById(minsu.getShangjiaId());
                if(shangjia != null){
                    BeanUtils.copyProperties( shangjia , view ,new String[]{ "id", "createDate"});//把级联的数据添加到view中,并排除id和创建时间字段
                    view.setShangjiaId(shangjia.getId());
                }
                //修改对应字典表字段
                dictionaryService.dictionaryConvert(view, request);
                return R.ok().put("data", view);
            }else {
                return R.error(511,"查不到数据");
            }
    }


    /**
    * 前端保存
    */
    @RequestMapping("/add")
    public R add(@RequestBody MinsuEntity minsu, HttpServletRequest request){
        logger.debug("add方法:,,Controller:{},,minsu:{}",this.getClass().getName(),minsu.toString());
        Wrapper<MinsuEntity> queryWrapper = new EntityWrapper<MinsuEntity>()
            .eq("shangjia_id", minsu.getShangjiaId())
            .eq("minsu_name", minsu.getMinsuName())
            .eq("minsu_types", minsu.getMinsuTypes())
            .eq("minsu_clicknum", minsu.getMinsuClicknum())
            .eq("shangxia_types", minsu.getShangxiaTypes())
            .eq("minsu_delete", minsu.getMinsuDelete())
            ;
        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        MinsuEntity minsuEntity = minsuService.selectOne(queryWrapper);
        if(minsuEntity==null){
            minsu.setMinsuDelete(1);
            minsu.setCreateTime(new Date());
        minsuService.insert(minsu);

            return R.ok();
        }else {
            return R.error(511,"表中有相同数据");
        }
    }

}
