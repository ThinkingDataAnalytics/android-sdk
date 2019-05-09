/**
 * Created by thinkingdata on 2017/11/20.
 */
;(function(root,factory) {
    if (typeof exports === 'object' && typeof module === 'object') {
        module.exports = factory();
    } else{
        factory();
    }
})(this,function(){
    try{

        var td = window['ThinkingDataAnalyticalTool'],isDeclare;
        if(td){
            td = window[td];
            isDeclare = true;
        }else{
            td = {};
            isDeclare = false;
        }
        if ((typeof td !== 'function' && typeof td !== 'object') || td.isLoadSDK) {
            return false;
        }
        td.isLoadSDK = true;

        if(typeof JSON!=='object'){JSON={}}(function(){'use strict';var rx_one=/^[\],:{}\s]*$/,rx_two=/\\(?:["\\\/bfnrt]|u[0-9a-fA-F]{4})/g,rx_three=/"[^"\\\n\r]*"|true|false|null|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?/g,rx_four=/(?:^|:|,)(?:\s*\[)+/g,rx_escapable=/[\\\"\u0000-\u001f\u007f-\u009f\u00ad\u0600-\u0604\u070f\u17b4\u17b5\u200c-\u200f\u2028-\u202f\u2060-\u206f\ufeff\ufff0-\uffff]/g,rx_dangerous=/[\u0000\u00ad\u0600-\u0604\u070f\u17b4\u17b5\u200c-\u200f\u2028-\u202f\u2060-\u206f\ufeff\ufff0-\uffff]/g;function f(n){return n<10?'0'+n:n}function this_value(){return this.valueOf()}if(typeof Date.prototype.toJSON!=='function'){Date.prototype.toJSON=function(){return isFinite(this.valueOf())?this.getUTCFullYear()+'-'+f(this.getUTCMonth()+1)+'-'+f(this.getUTCDate())+'T'+f(this.getUTCHours())+':'+f(this.getUTCMinutes())+':'+f(this.getUTCSeconds())+'Z':null};Boolean.prototype.toJSON=this_value;Number.prototype.toJSON=this_value;String.prototype.toJSON=this_value}var gap,indent,meta,rep;function quote(string){rx_escapable.lastIndex=0;return rx_escapable.test(string)?'"'+string.replace(rx_escapable,function(a){var c=meta[a];return typeof c==='string'?c:'\\u'+('0000'+a.charCodeAt(0).toString(16)).slice(-4)})+'"':'"'+string+'"'}function str(key,holder){var i,k,v,length,mind=gap,partial,value=holder[key];if(value&&typeof value==='object'&&typeof value.toJSON==='function'){value=value.toJSON(key)}if(typeof rep==='function'){value=rep.call(holder,key,value)}switch(typeof value){case'string':return quote(value);case'number':return isFinite(value)?String(value):'null';case'boolean':case'null':return String(value);case'object':if(!value){return'null'}gap+=indent;partial=[];if(Object.prototype.toString.apply(value)==='[object Array]'){length=value.length;for(i=0;i<length;i+=1){partial[i]=str(i,value)||'null'}v=partial.length===0?'[]':gap?'[\n'+gap+partial.join(',\n'+gap)+'\n'+mind+']':'['+partial.join(',')+']';gap=mind;return v}if(rep&&typeof rep==='object'){length=rep.length;for(i=0;i<length;i+=1){if(typeof rep[i]==='string'){k=rep[i];v=str(k,value);if(v){partial.push(quote(k)+(gap?': ':':')+v)}}}}else{for(k in value){if(Object.prototype.hasOwnProperty.call(value,k)){v=str(k,value);if(v){partial.push(quote(k)+(gap?': ':':')+v)}}}}v=partial.length===0?'{}':gap?'{\n'+gap+partial.join(',\n'+gap)+'\n'+mind+'}':'{'+partial.join(',')+'}';gap=mind;return v}}if(typeof JSON.stringify!=='function'){meta={'\b':'\\b','\t':'\\t','\n':'\\n','\f':'\\f','\r':'\\r','"':'\\"','\\':'\\\\'};JSON.stringify=function(value,replacer,space){var i;gap='';indent='';if(typeof space==='number'){for(i=0;i<space;i+=1){indent+=' '}}else if(typeof space==='string'){indent=space}rep=replacer;if(replacer&&typeof replacer!=='function'&&(typeof replacer!=='object'||typeof replacer.length!=='number')){throw new Error('JSON.stringify')}return str('',{'':value})}}if(typeof JSON.parse!=='function'){JSON.parse=function(text,reviver){var j;function walk(holder,key){var k,v,value=holder[key];if(value&&typeof value==='object'){for(k in value){if(Object.prototype.hasOwnProperty.call(value,k)){v=walk(value,k);if(v!==undefined){value[k]=v}else{delete value[k]}}}}return reviver.call(holder,key,value)}text=String(text);rx_dangerous.lastIndex=0;if(rx_dangerous.test(text)){text=text.replace(rx_dangerous,function(a){return'\\u'+('0000'+a.charCodeAt(0).toString(16)).slice(-4)})}if(rx_one.test(text.replace(rx_two,'@').replace(rx_three,']').replace(rx_four,''))){j=eval('('+text+')');return typeof reviver==='function'?walk({'':j},''):j}throw new SyntaxError('JSON.parse')}}}());
        var ArrayProto = Array.prototype;
        var FuncProto = Function.prototype;
        var ObjProto = Object.prototype;
        var slice = ArrayProto.slice;
        var toString = ObjProto.toString;
        var hasOwnProperty = ObjProto.hasOwnProperty;
        var errors = [];
        var isFirstVisitor = false;
        var nativeForEach = ArrayProto.forEach,breaker = {};
        var tdEach = function(obj, iterator, context) {
            if (obj == null) {
                return false;
            }
            if (nativeForEach && obj.forEach === nativeForEach) {
                obj.forEach(iterator, context);
            } else if (obj.length === +obj.length) {
                for (var i = 0, l = obj.length; i < l; i++) {
                    if (i in obj && iterator.call(context, obj[i], i, obj) === breaker) {
                        return false;
                    }
                }
            } else {
                for (var key in obj) {
                    if (hasOwnProperty.call(obj, key)) {
                        if (iterator.call(context, obj[key], key, obj) === breaker) {
                            return false;
                        }
                    }
                }
            }
        };
        var tdCommon = {
            _getAgent:function () {
                var agent = navigator ? navigator : {};

            },
            _getReferrer:function(referrer){
                var referrer = referrer || document.referrer;
                if(typeof referrer !== 'string'){
                    return '取值异常_referrer异常_' + String(referrer);
                }
                if (referrer.indexOf("https://www.baidu.com/") === 0) {
                    referrer = referrer.split('?')[0];
                }
                referrer = referrer.slice(0, td.param.maxReferrerStringLength);
                return (typeof referrer === 'string' ? referrer : '' );
            },
            _extend:function(obj) {
                tdEach(slice.call(arguments, 1), function(source) {
                    for (var prop in source) {
                        if (source[prop] !== void 0) {
                            obj[prop] = source[prop];
                        }
                    }
                });
                return obj;
            },
            _coverExtend:function(obj) {
                tdEach(slice.call(arguments, 1), function(source) {
                    for (var prop in source) {
                        if (source[prop] !== void 0 && obj[prop] === void 0) {
                            obj[prop] = source[prop];
                        }
                    }
                });
                return obj;
            },
            _date:{
                _format:function(d) {
                    function pad(n) {
                        return n < 10 ? '0' + n : n;
                    }
                    function padMilliseconds(n) {
                        if(n < 10){
                            return '00' + n;
                        }else if(n < 100){
                            return '0' + n;
                        }else{
                            return n;
                        }
                    }
                    return d.getFullYear() + '-'
                        + pad(d.getMonth() + 1) + '-'
                        + pad(d.getDate()) + ' '
                        + pad(d.getHours()) + ':'
                        + pad(d.getMinutes()) + ':'
                        + pad(d.getSeconds()) + '.'
                        + padMilliseconds(d.getMilliseconds());
                }
            },
            _code:{
                _utf8Encode:function(string) {
                    string = (string + '').replace(/\r\n/g, '\n').replace(/\r/g, '\n');
                    var utftext = '', start, end;
                    var stringl = 0, n;
                    start = end = 0;
                    stringl = string.length;
                    for (n = 0; n < stringl; n++) {
                        var c1 = string.charCodeAt(n);
                        var enc = null;
                        if (c1 < 128) {
                            end++;
                        } else if ((c1 > 127) && (c1 < 2048)) {
                            enc = String.fromCharCode((c1 >> 6) | 192, (c1 & 63) | 128);
                        } else {
                            enc = String.fromCharCode((c1 >> 12) | 224, ((c1 >> 6) & 63) | 128, (c1 & 63) | 128);
                        }
                        if (enc !== null) {
                            if (end > start) {
                                utftext += string.substring(start, end);
                            }
                            utftext += enc;
                            start = end = n + 1;
                        }
                    }
                    if (end > start) {
                        utftext += string.substring(start, string.length);
                    }
                    return utftext;
                },
                _base64Encode:function(data) {
                    var b64 = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=';
                    var o1, o2, o3, h1, h2, h3, h4, bits, i = 0, ac = 0, enc = '', tmp_arr = [];
                    if (!data) {
                        return data;
                    }
                    data = this._utf8Encode(data);
                    do {
                        o1 = data.charCodeAt(i++);
                        o2 = data.charCodeAt(i++);
                        o3 = data.charCodeAt(i++);
                        bits = o1 << 16 | o2 << 8 | o3;
                        h1 = bits >> 18 & 0x3f;
                        h2 = bits >> 12 & 0x3f;
                        h3 = bits >> 6 & 0x3f;
                        h4 = bits & 0x3f;
                        tmp_arr[ac++] = b64.charAt(h1) + b64.charAt(h2) + b64.charAt(h3) + b64.charAt(h4);
                    } while (i < data.length);
                    enc = tmp_arr.join('');
                    switch (data.length % 3) {
                        case 1:
                            enc = enc.slice(0, -2) + '==';
                            break;
                        case 2:
                            enc = enc.slice(0, -1) + '=';
                            break;
                    }
                    return enc;
                },

                _decodeURIComponent:function(val){
                    var result = '';
                    try{
                        result = decodeURIComponent(val);
                    }catch(e){
                        result = val;
                    }
                    return result;
                },
                _encodeURIComponent:function(val){
                    var result = '';
                    try{
                        result = encodeURIComponent(val);
                    }catch(e){
                        result = val;
                    }
                    return result;
                },
                _hashCode:function(str){
                    if(typeof str !== 'string'){
                        return 0;
                    }
                    var hash = 0;
                    var char = null;
                    if (str.length == 0) {
                        return hash;
                    }
                    for (var i = 0; i < str.length; i++) {
                        char = str.charCodeAt(i);
                        hash = ((hash<<5)-hash)+char;
                        hash = hash & hash;
                    }
                    return hash;
                }
            },
            _check:{
                _isEmptyObject:function(obj) {
                    if (tdCommon._check._isObject(obj)) {
                        for (var key in obj) {
                            if (hasOwnProperty.call(obj, key)) {
                                return false;
                            }
                        }
                        return true;
                    }
                    return false;
                },
                _isObject:function(obj) {
                    return (toString.call(obj) == '[object Object]') && (obj != null);
                },
                _isArray:function(obj) {
                    return toString.call(obj) === '[object Array]';
                },
                _isString:function(obj) {
                    return toString.call(obj) == '[object String]';
                },
                _isDate:function(obj) {
                    return toString.call(obj) == '[object Date]';
                },
                _isNumber:function(obj) {
                    return (toString.call(obj) == '[object Number]' && /[\d\.]+/.test(String(obj)));
                },
                _isBoolean:function(obj) {
                    return toString.call(obj) == '[object Boolean]';
                },
                _isJSONString:function(str) {
                    try {
                        JSON.parse(str);
                    } catch (e) {
                        return false;
                    }
                    return true;
                }
            },
            _format:{

                _formatString:function(str) {
                    if (str.length > td.param.maxStringLength) {
                        tdLog._info('字符串长度超过限制，已经做截取--' + str);
                        return str.slice(0, td.param.maxStringLength);
                    } else {
                        return str;
                    }
                },
                _formatJsonString:function(obj){
                    try{
                        return JSON.stringify(obj, null, '  ');
                    }catch(e){
                        return JSON.stringify(obj);
                    }
                },
                _searchObjDate:function(o) {
                    if (tdCommon._check._isObject(o)) {
                        tdEach(o, function(a, b) {
                            if (tdCommon._check._isObject(a)) {
                                tdCommon._format._searchObjDate(o[b]);
                            } else {
                                if (tdCommon._check._isDate(a)) {
                                    o[b] = tdCommon._date._format(a);
                                }
                            }
                        });
                    }
                },

                _searchObjString:function(o) {
                    if (tdCommon._check._isObject(o)) {
                        tdEach(o, function(a, b) {
                            if (tdCommon._check._isObject(a)) {
                                tdCommon._format._searchObjString(o[b]);
                            } else {
                                if (tdCommon._check._isString(a)) {
                                    o[b] = tdCommon._format._formatString(a);
                                }
                            }
                        });
                    }
                },
                _stripProperties:function(p) {
                    if (!tdCommon._check._isObject(p)) {
                        return p;
                    }
                    tdEach(p, function(v, k) {

                        if (tdCommon._check._isArray(v)) {
                            var temp = [];
                            tdEach(v, function(arrv) {
                                if (tdCommon._check._isString(arrv)) {
                                    temp.push(arrv);
                                } else {
                                    tdLog._info('您的数据-',k, v, '的数组里的值必须是字符串,已经将其删除');
                                }
                            });
                            if (temp.length !== 0) {
                                p[k] = temp;
                            } else {
                                delete p[k];
                                tdLog._info('已经删除空的数组');
                            }
                        }
                        if (!(tdCommon._check._isString(v) || tdCommon._check._isNumber(v) || tdCommon._check._isDate(v) || tdCommon._check._isBoolean(v) || tdCommon._check._isArray(v))) {
                            tdLog._info('您的数据-',k, v, '-格式不满足要求，我们已经将其删除');
                            delete p[k];
                        }
                    });
                    return p;
                }
            },
            _send:{
                _xhr:function(cors) {
                    if (cors) {
                        var xhr = new XMLHttpRequest();
                        if ("withCredentials" in xhr) {
                            return xhr;
                        } else if (typeof XDomainRequest != "undefined") {
                            return new XDomainRequest();
                        } else {
                            return xhr;
                        }
                    } else {
                        if (XMLHttpRequest) {
                            return new XMLHttpRequest();
                        }
                        if (window.ActiveXObject) {
                            try {
                                return new ActiveXObject('Msxml2.XMLHTTP')
                            } catch (d) {
                                try {
                                    return new ActiveXObject('Microsoft.XMLHTTP')
                                } catch (d) {
                                }
                            }
                        }
                    }
                },
                _ajax:function(param) {
                    function getJSON(data) {
                        try {
                            return JSON.parse(data);
                        } catch (e) {
                            return {};
                        }
                    }
                    var g = tdCommon._send._xhr(param.cors);
                    if (!param.type) {
                        param.type = param.data ? 'POST' : 'GET';
                    }
                    param = tdCommon._extend({
                        success: function() {},
                        error: function() {}
                    }, param);
                    g.onreadystatechange = function() {
                        if (g.readyState == 4) {
                            if ((g.status >= 200 && g.status < 300) || g.status == 304) {
                                param.success(getJSON(g.responseText));
                            } else {
                                param.error(getJSON(g.responseText), g.status);
                            }
                            g.onreadystatechange = null;
                            g.onload = null;
                        }
                    };
                    g.open(param.type, param.url, true);
                    try {
                        g.withCredentials = true;
                        if (tdCommon._check._isObject(para.header)) {
                            for (var i in para.header) {
                                g.setRequestHeader(i, para.header[i]);
                            }
                        }
                        if (param.data) {
                            g.setRequestHeader("X-Requested-With", "XMLHttpRequest");
                            if (para.contentType === 'application/json') {
                                g.setRequestHeader("Content-type", "application/json; charset=UTF-8");
                            } else {
                                g.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
                            }
                        }
                    } catch (e) {
                    }
                    g.send(param.data || null);
                }
            }
        };
        var tdUUID = (function() {
            var T = function() {
                var d = 1 * new Date(), i = 0;
                while (d == 1 * new Date()) {
                    i++;
                }
                return d.toString(16) + i.toString(16);
            };
            var R = function() {
                return Math.random().toString(16).replace('.', '');
            };
            var UA = function(n) {
                var ua = navigator.userAgent, i, ch, buffer = [], ret = 0;
                function xor(result, byte_array) {
                    var j, tmp = 0;
                    for (j = 0; j < byte_array.length; j++) {
                        tmp |= (buffer[j] << j * 8);
                    }
                    return result ^ tmp;
                }
                for (i = 0; i < ua.length; i++) {
                    ch = ua.charCodeAt(i);
                    buffer.unshift(ch & 0xFF);
                    if (buffer.length >= 4) {
                        ret = xor(ret, buffer);
                        buffer = [];
                    }
                }
                if (buffer.length > 0) {
                    ret = xor(ret, buffer);
                }
                return ret.toString(16);
            };
            return function() {
                var se = String(screen.height * screen.width);
                if (se && /\d{5,}/.test(se)) {
                    se = se.toString(16);
                } else {
                    se = String(Math.random() * 31242).replace('.', '').slice(0, 8);
                }
                var val = (T() + '-' + R() + '-' + UA() + '-' + se + '-' + T());
                if(val){
                    return val;
                }else{
                    return (String(Math.random()) + String(Math.random()) + String(Math.random())).slice(2, 15);
                }
            };
        })();
        var tdUrl = (function() {
            function _t() {
                return new RegExp(/(.*?)\.?([^\.]*?)\.(com|net|org|biz|ws|in|me|co\.uk|co|org\.uk|ltd\.uk|plc\.uk|me\.uk|edu|mil|br\.com|cn\.com|eu\.com|hu\.com|no\.com|qc\.com|sa\.com|se\.com|se\.net|us\.com|uy\.com|ac|co\.ac|gv\.ac|or\.ac|ac\.ac|af|am|as|at|ac\.at|co\.at|gv\.at|or\.at|asn\.au|com\.au|edu\.au|org\.au|net\.au|id\.au|be|ac\.be|adm\.br|adv\.br|am\.br|arq\.br|art\.br|bio\.br|cng\.br|cnt\.br|com\.br|ecn\.br|eng\.br|esp\.br|etc\.br|eti\.br|fm\.br|fot\.br|fst\.br|g12\.br|gov\.br|ind\.br|inf\.br|jor\.br|lel\.br|med\.br|mil\.br|net\.br|nom\.br|ntr\.br|odo\.br|org\.br|ppg\.br|pro\.br|psc\.br|psi\.br|rec\.br|slg\.br|tmp\.br|tur\.br|tv\.br|vet\.br|zlg\.br|br|ab\.ca|bc\.ca|mb\.ca|nb\.ca|nf\.ca|ns\.ca|nt\.ca|on\.ca|pe\.ca|qc\.ca|sk\.ca|yk\.ca|ca|cc|ac\.cn|net\.cn|com\.cn|edu\.cn|gov\.cn|org\.cn|bj\.cn|sh\.cn|tj\.cn|cq\.cn|he\.cn|nm\.cn|ln\.cn|jl\.cn|hl\.cn|js\.cn|zj\.cn|ah\.cn|gd\.cn|gx\.cn|hi\.cn|sc\.cn|gz\.cn|yn\.cn|xz\.cn|sn\.cn|gs\.cn|qh\.cn|nx\.cn|xj\.cn|tw\.cn|hk\.cn|mo\.cn|cn|cx|cz|de|dk|fo|com\.ec|tm\.fr|com\.fr|asso\.fr|presse\.fr|fr|gf|gs|co\.il|net\.il|ac\.il|k12\.il|gov\.il|muni\.il|ac\.in|co\.in|org\.in|ernet\.in|gov\.in|net\.in|res\.in|is|it|ac\.jp|co\.jp|go\.jp|or\.jp|ne\.jp|ac\.kr|co\.kr|go\.kr|ne\.kr|nm\.kr|or\.kr|li|lt|lu|asso\.mc|tm\.mc|com\.mm|org\.mm|net\.mm|edu\.mm|gov\.mm|ms|nl|no|nu|pl|ro|org\.ro|store\.ro|tm\.ro|firm\.ro|www\.ro|arts\.ro|rec\.ro|info\.ro|nom\.ro|nt\.ro|se|si|com\.sg|org\.sg|net\.sg|gov\.sg|sk|st|tf|ac\.th|co\.th|go\.th|mi\.th|net\.th|or\.th|tm|to|com\.tr|edu\.tr|gov\.tr|k12\.tr|net\.tr|org\.tr|com\.tw|org\.tw|net\.tw|ac\.uk|uk\.com|uk\.net|gb\.com|gb\.net|vg|sh|kz|ch|info|ua|gov|name|pro|ie|hk|com\.hk|org\.hk|net\.hk|edu\.hk|us|tk|cd|by|ad|lv|eu\.lv|bz|es|jp|cl|ag|mobi|eu|co\.nz|org\.nz|net\.nz|maori\.nz|iwi\.nz|io|la|md|sc|sg|vc|tw|travel|my|se|tv|pt|com\.pt|edu\.pt|asia|fi|com\.ve|net\.ve|fi|org\.ve|web\.ve|info\.ve|co\.ve|tel|im|gr|ru|net\.ru|org\.ru|hr|com\.hr|ly|xyz)$/);
            }
            function _d(s) {
                return tdCommon._code._decodeURIComponent(s.replace(/\+/g, ' '));
            }
            function _i(arg, str) {
                var sptr = arg.charAt(0),
                    split = str.split(sptr);
                if (sptr === arg) { return split; }
                arg = parseInt(arg.substring(1), 10);
                return split[arg < 0 ? split.length + arg : arg - 1];
            }
            function _f(arg, str) {
                var sptr = arg.charAt(0),
                    split = str.split('&'),
                    field = [],
                    params = {},
                    tmp = [],
                    arg2 = arg.substring(1);

                for (var i = 0, ii = split.length; i < ii; i++) {
                    field = split[i].match(/(.*?)=(.*)/);
                    // TODO: regex should be able to handle this.
                    if ( ! field) {
                        field = [split[i], split[i], ''];
                    }
                    if (field[1].replace(/\s/g, '') !== '') {
                        field[2] = _d(field[2] || '');
                        // If we have a match just return it right away.
                        if (arg2 === field[1]) { return field[2]; }
                        // Check for array pattern.
                        tmp = field[1].match(/(.*)\[([0-9]+)\]/);
                        if (tmp) {
                            params[tmp[1]] = params[tmp[1]] || [];
                            params[tmp[1]][tmp[2]] = field[2];
                        }
                        else {
                            params[field[1]] = field[2];
                        }
                    }
                }
                if (sptr === arg) { return params; }
                return params[arg2];
            }
            return function(arg, url) {
                var _l = {}, tmp, tmp2;
                if (arg === 'tld?') { return _t(); }
                url = url || window.location.toString();
                if ( ! arg) { return url; }
                arg = arg.toString();
                if (tmp = url.match(/^mailto:([^\/].+)/)) {
                    _l.protocol = 'mailto';
                    _l.email = tmp[1];
                }else {
                    // Ignore Hashbangs.
                    if (tmp = url.match(/(.*?)\/#\!(.*)/)) {
                        url = tmp[1] + tmp[2];
                    }
                    // Hash.
                    if (tmp = url.match(/(.*?)#(.*)/)) {
                        _l.hash = tmp[2];
                        url = tmp[1];
                    }
                    // Return hash parts.
                    if (_l.hash && arg.match(/^#/)) { return _f(arg, _l.hash); }
                    // Query
                    if (tmp = url.match(/(.*?)\?(.*)/)) {
                        _l.query = tmp[2];
                        url = tmp[1];
                    }
                    // Return query parts.
                    if (_l.query && arg.match(/^\?/)) { return _f(arg, _l.query); }
                    // Protocol.
                    if (tmp = url.match(/(.*?)\:?\/\/(.*)/)) {
                        _l.protocol = tmp[1].toLowerCase();
                        url = tmp[2];
                    }
                    // Path.
                    if (tmp = url.match(/(.*?)(\/.*)/)) {
                        _l.path = tmp[2];
                        url = tmp[1];
                    }
                    // Clean up path.
                    _l.path = (_l.path || '').replace(/^([^\/])/, '/$1').replace(/\/$/, '');
                    // Return path parts.
                    if (arg.match(/^[\-0-9]+$/)) { arg = arg.replace(/^([^\/])/, '/$1'); }
                    if (arg.match(/^\//)) { return _i(arg, _l.path.substring(1)); }
                    // File.
                    tmp = _i('/-1', _l.path.substring(1));
                    if (tmp && (tmp = tmp.match(/(.*?)\.(.*)/))) {
                        _l.file = tmp[0];
                        _l.filename = tmp[1];
                        _l.fileext = tmp[2];
                    }
                    // Port.
                    if (tmp = url.match(/(.*)\:([0-9]+)$/)) {
                        _l.port = tmp[2];
                        url = tmp[1];
                    }
                    // Auth.
                    if (tmp = url.match(/(.*?)@(.*)/)) {
                        _l.auth = tmp[1];
                        url = tmp[2];
                    }
                    // User and pass.
                    if (_l.auth) {
                        tmp = _l.auth.match(/(.*)\:(.*)/);

                        _l.user = tmp ? tmp[1] : _l.auth;
                        _l.pass = tmp ? tmp[2] : undefined;
                    }
                    // Hostname.
                    _l.hostname = url.toLowerCase();
                    // Return hostname parts.
                    if (arg.charAt(0) === '.') { return _i(arg, _l.hostname); }
                    // Domain, tld and sub domain.
                    if (_t()) {
                        tmp = _l.hostname.match(_t());
                        if (tmp) {
                            _l.tld = tmp[3];
                            _l.domain = tmp[2] ? tmp[2] + '.' + tmp[3] : undefined;
                            _l.sub = tmp[1] || undefined;
                        }
                    }
                    // Set port and protocol defaults if not set.
                    _l.port = _l.port || (_l.protocol === 'https' ? '443' : '80');
                    _l.protocol = _l.protocol || (_l.port === '443' ? 'https' : 'http');
                }
                // Return arg.
                if (arg in _l) { return _l[arg]; }
                // Return everything.
                if (arg === '{}') { return _l; }
                // Default to undefined for no match.
                return '';
            };
        })();
        var tdStore = {
            _sessionState: {},
            _state: {},
            _getProps: function() {
                return this._state.props || {};
            },
            _getSessionProps: function() {
                return this._sessionState;
            },
            _getDistinctId: function() {
                return this._state.distinct_id;
            },
            _getAccountId: function() {
                return this._state.account_id;
            },
            _getDeviceId: function() {
                return this._state.device_id;
            },
            _getFirstId: function(){
                return this._state.first_id;
            },
            _setDeviceId: function(uuid){
                var device_id = null;
                var ds = tdClient._cookie._get(td.param.cookieCross);
                var state = {};
                if (ds != null && tdCommon._check._isJSONString(ds)) {
                    state = JSON.parse(ds);
                    if(state.device_id) {
                        device_id = state.device_id;
                    }
                }
                device_id = device_id || uuid;
                this._state.device_id = device_id;
                if(td.param.crossSubDomain === true){
                    tdStore._set('device_id',device_id);
                }else{
                    tdClient._cookie._set(td.param.cookieCross,JSON.stringify(state),null,true);
                }
            },
            _toState: function(ds) {
                var state = null;
                if (ds != null && tdCommon._check._isJSONString(ds)) {
                    state = JSON.parse(ds);
                    this._state = tdCommon._extend(state);
                    if (state.distinct_id) {
                        if(typeof(state.props) === 'object'){
                            for(var key in state.props){
                                if(typeof state.props[key] === 'string'){
                                    state.props[key] = state.props[key].slice(0, td.param.maxReferrerStringLength);
                                }
                            }
                            this._save();
                        }
                    } else {
                        this._set('distinct_id', tdUUID());
                        errors.push('toStateParseDistinctError');
                    }
                } else {
                    this._set('distinct_id', tdUUID());
                    errors.push('toStateParseError');
                }
            },
            _initSessionState: function() {
                var ds = tdClient._cookie._get(td.param.sessionName);
                var state = null;
                if (ds !== null && (typeof (state = JSON.parse(ds)) === 'object')) {
                    this._sessionState = state || {};
                }
            },
            _set: function(name, value) {
                this._state = this._state || {};
                this._state[name] = value;
                this._save();
            },
            _sessionSave: function(props) {
                this._sessionState = props;
                tdClient._cookie._set(td.param.sessionName, JSON.stringify(this._sessionState), 0);
            },
            _save: function() {
                tdClient._cookie._set(tdClient._cookie._getCookieName(), JSON.stringify(this._state), 73000, td.param.crossSubDomain);
            },
            _init: function() {
                if (!navigator.cookieEnabled) {
                    errors.push('cookieNotEnable');
                    if (!tdClient._localStorage.isSupport) {
                        errors.push('localStorageNotEnable');
                    }
                }
                this._initSessionState();
                var uuid = tdUUID();
                var cross = tdClient._cookie._get(tdClient._cookie._getCookieName());
                if (cross === null) {
                    this._set('distinct_id', uuid);
                } else {
                    this._toState(cross);
                }
                tdStore._setDeviceId(uuid);
                tdConstruct._initProperties();
            }
        };

        var tdClient = {
            _cookie:{
                _get: function(name) {
                    var nameEQ = name + '=';
                    var ca = document.cookie.split(';');
                    for (var i = 0; i < ca.length; i++) {
                        var c = ca[i];
                        while (c.charAt(0) == ' ') {
                            c = c.substring(1, c.length);
                        }
                        if (c.indexOf(nameEQ) == 0) {
                            return tdCommon._code._decodeURIComponent(c.substring(nameEQ.length, c.length));
                        }
                    }
                    return null;
                },
                _set: function(name, value, days, crossSubDomain, isSecure) {
                    crossSubDomain = typeof crossSubDomain === 'undefined' ? td.param.crossSubDomain : crossSubDomain;
                    var cDomain = '', expires = '', secure = '';
                    days = days == null ? 73000 : days;
                    if (crossSubDomain) {
                        var domain = tdUrl('domain',location.href);
                        cDomain = ((domain) ? '; domain=.' + domain : '');
                    }
                    if (days !== 0) {
                        var date = new Date();
                        if (String(days).slice(-1) === 's') {
                            date.setTime(date.getTime() + (Number(String(days).slice(0, -1)) * 1000));
                        } else {
                            date.setTime(date.getTime() + (days * 24 * 60 * 60 * 1000));
                        }
                        expires = '; expires=' + date.toGMTString();
                    }
                    if (isSecure) {
                        secure = '; secure';
                    }
                    document.cookie = name + '=' + encodeURIComponent(value) + expires
                        + '; path=/' + cDomain + secure;
                },
                _remove: function(name, crossSubDomain) {
                    crossSubDomain = typeof crossSubDomain === 'undefined' ? td.param.crossSubDomain : crossSubDomain;
                    td.cookie._set(name, '', -1, crossSubDomain);
                },
                _getCookieName: function(name_prefix){
                    var sub = '';
                    if(name_prefix){
                        name_prefix = '_'+name_prefix;
                    }else{
                        name_prefix = '';
                    }
                    if(td.param.crossSubDomain === false){
                        sub = tdUrl('sub',location.href);
                        if(typeof sub === 'string' && sub !== ''){
                            sub = td.param.cookiePrefix + name_prefix + '_' + sub;
                        }else{
                            sub = td.param.cookiePrefix+'_root' + name_prefix;
                        }
                    }else{
                        sub = td.param.cookiePrefix+'_cross' + name_prefix;
                    }
                    return sub;
                },
                _getNewUser: function(){
                    var prefix = 'new_user';
                    if(this._get('ThinkingData_is_new_user') !== null || this._get(this._getCookieName(prefix)) !== null){
                        return true;
                    }else{
                        return false;
                    }
                }
            },
            _localStorage:{
                get: function(name) {
                    return window.localStorage.getItem(name);
                },
                parse: function(name) {
                    var storedValue;
                    try {
                        storedValue = JSON.parse(tdClient._localStorage.get(name)) || null;
                    } catch (err) {
                    }
                    return storedValue;
                },
                set: function(name, value) {
                    window.localStorage.setItem(name, value);
                },
                remove: function(name) {
                    window.localStorage.removeItem(name);
                },
                isSupport: function() {
                    var supported = true;
                    try {
                        var key = '__thinkingdatasupport__';
                        var val = 'testIsSupportStorage';
                        tdClient._localStorage.set(key, val);
                        if (tdClient._localStorage.get(key) !== val) {
                            supported = false;
                        }
                        tdClient._localStorage.remove(key);
                    } catch (err) {
                        supported = false;
                    }
                    return supported;
                }
            },
            _sessionStorage:{
                isSupport:function(){
                    var supported = true;
                    var key = '__thinkingdatasupport__';
                    var val = 'testIsSupportStorage';
                    try{
                        if(sessionStorage && sessionStorage.setItem){
                            sessionStorage.setItem(key,val);
                            sessionStorage.removeItem(key,val);
                            supported = true;
                        }else{
                            supported = false;
                        }
                    }catch(e){
                        supported = false;
                    }
                    return supported;
                }
            }
        };
        var tdLog = {
            _info:function () {
                if (!td.param.showLog) {
                    return false;
                }
                if(td.param.showLog === true || td.param.showLog === 'string'){
                    arguments[0] = tdCommon._format._formatJsonString(arguments[0]);
                }
                if (typeof console === 'object' && console.log) {
                    try {
                        return console.log.apply(console, arguments);
                    } catch (e) {
                        console.log(arguments[0]);
                    }
                }
            }
        };

        var tdEvent = {
            _checkOption:{
                regChecks: {
                    regName: /^[a-zA-Z_#$][a-zA-Z\d_$]{0,99}$/
                },
                arrayChecks:['distinct_id','account_id','time','properties','referrer','referrer_host','url','url_path','title','event_name','type'],
                check: function(a, b) {
                    if (typeof this[a] === 'string') {
                        return this[this[a]](b);
                    } else {
                        return this[a](b);
                    }
                },
                event: function(s) {
                    if (!tdCommon._check._isString(s) || !tdEvent._checkOption['regChecks']['regName'].test(s)) {
                        tdLog._info('请检查参数格式,必须是字符串,且eventName必须是字符串_开头,且不能是系统保留字');
                        return true;
                    } else {
                        return true;
                    }
                },
                checkPropertiesKey: function(obj) {
                    var me = this, flag = true;
                    tdEach(obj, function(content, key) {
                        if (!me.regChecks.regName.test(key)) {
                            flag = false;
                        }
                    });
                    return flag;
                },
                properties: function(p) {
                    tdCommon._format._stripProperties(p);
                    if (p) {
                        if (tdCommon._check._isObject(p)) {
                            if (this.checkPropertiesKey(p)) {
                                return true;
                            } else {
                                tdLog._info('properties里的key必须是由字符串数字_组成，且不能是系统保留字');
                                return true;
                            }
                        } else {
                            tdLog._info('properties可以没有，但有的话必须是对象');
                            return true;
                        }
                    } else {
                        return true;
                    }
                },
                propertiesMust: function(p) {
                    tdCommon._format._stripProperties(p);
                    if (p === undefined || !tdCommon._check._isObject(p) || tdCommon._check._isEmptyObject(p)) {
                        logger.info('properties必须是对象且有值');
                        return true;
                    } else {
                        if (this.checkPropertiesKey(p)) {
                            return true;
                        } else {
                            logger.info('properties里的key必须是由字符串数字_组成，且不能是系统保留字');
                            return true;
                        }
                    }
                },
                distinct_id: function(id) {
                    if (tdCommon._check._isString(id) && /^.{1,255}$/.test(id)) {
                        return true;
                    } else {
                        tdLog._info('distinct_id必须是不能为空，且小于255位的字符串');
                        return false;
                    }
                },
                account_id: function(id) {
                    if (tdCommon._check._isString(id) && /^.{1,255}$/.test(id)) {
                        return true;
                    } else {
                        tdLog._info('distinct_id必须是不能为空，且小于255位的字符串');
                        return false;
                    }
                }
            },
            _check:function(p) {
                var flag = true;
                for (var i in p) {
                    if (!this._checkOption.check(i, p[i])) {
                        return false;
                    }
                }
                return flag;
            },
            _send:function(p, callback) {
                var data = {
                    'data': [{
                        "#type": p.type,
                        "#time": tdCommon._date._format(new Date()),
                        "#event_name": p.event,
                        "#distinct_id": tdStore._state.distinct_id,
                        "properties": {}
                    }]
                };

                if (p.type == "user_set" || p.type == "user_setOnce" || p.type == "user_add"|| p.type == "user_del") { }
                else
                {
                    data.data[0]['properties'] = tdCommon._extend({}, td.defaultProp, tdConstruct.currentProps);
                }

                if(tdStore._state.account_id){
                    data.data[0]['#account_id'] = tdStore._state.account_id;
                }

                if (tdCommon._check._isObject(p.properties) && !tdCommon._check._isEmptyObject(p.properties)) {
                    tdCommon._extend(data.data[0].properties, p.properties);
                }
                if (tdCommon._check._isObject(callback)) {
                    tdCommon._extend(data.lib, callback);
                }

                tdCommon._format._searchObjDate(data.data[0]);
                // tdCommon._format._searchObjString(data.data[0]);

                
                tdSend._getSendCall(data, callback);
            }
        };
        var tdUser = {
            isFirstVisit: false,
            _checkIsAddSign: function(data) {
                if (data.type === 'track') {
                    if (tdClient._cookie._getNewUser()) {
                        data.properties.$is_first_day = true;
                    } else {
                        data.properties.$is_first_day = false;
                    }
                }
            },
            _checkIsFirstTime: function(data) {
                if (data.type === 'track' && data.event === 'pageview') {
                    if (this.isFirstVisit) {
                        data.properties.$is_first_time = true;
                        this.isFirstVisit = false;
                    } else {
                        data.properties.$is_first_time = false;
                    }
                }
            },
            _storeInitCheck: function() {
              
            },
            _trackSignup:function(id, e, p, c) {
                if (tdEvent._check({account_id: id, event: e, properties: p})) {
                    tdStore._set('account_id', id);
                }
            }
        };

        var tdConstruct = {
            _paramDefault:{
                name: 'td',
                appId:'',
                send_method:'image',
                sessionName: 'ThinkingDataSession',
                cookieName: 'ThinkingDataCookie',
                cookiePrefix: 'ThinkingDataJSSDK',
                cookieCross: 'ThinkingDataJSSDKCross',
                maxReferrerStringLength: 200,
                maxStringLength: 500,
                crossSubDomain: true,
                showLog: true,
                debugModeUpload:false,
                debugMode: false,
                debugModeUrl:'',
                sessionTime: 0,
                useClientTime: false,
                isTrackLink:true,
                callbackTimeout: 1000,
                useAppTrack:true
            },
            _paramInit:function(param){
                td.param = param || td.param || {};
                var i;
                for (i in this._paramDefault) {
                    if (td.param[i] === void 0) {
                        td.param[i] = this._paramDefault[i];
                    }
                }

                if(typeof td.param.serverUrl === 'string'){
                    td.param.debugModeUrl = td.param.debugModeUrl || td.param.serverUrl.replace('td.gif', 'debug');
                }

                if(td.param.send_method !== 'image' && td.param.send_method !== 'ajax'){
                    td.param.send_method = 'image';
                }
            },
            _initPage: function() {
                var referrer = tdCommon._getReferrer();
                var referrer_host = referrer ? tdUrl('hostname',referrer) : referrer;
                var referrer_domain = referrer ? tdUrl('domain',referrer) : referrer;
                var url = location.href;
                var url_host = url ? tdUrl('hostname',url) : url;
                var url_domain = url ? tdUrl('domain',url) : url;

                td.pageProp = {
                    referrer: referrer,
                    referrer_host: referrer_host,
                    referrer_domain: referrer_domain,
                    url: url,
                    url_host: url_host,
                    url_domain: url_domain
                };
            },
            _initProperties:function () {
                var screen_height = Number(screen.height) || 0;
                var screen_width = Number(screen.width) || 0;
                var os_name = navigator.appName || '';
                var os_version = navigator.appVersion || '';
                var os_platform = navigator.platform || '';
                var browser_info = this._getBrowser();

                td.defaultProp = {
                    "#lib_version": "1.0.6",
                    "#lib": "js",
                    "#device_id": tdStore._state.device_id,
                    "#screen_height": screen_height,
                    "#screen_width": screen_width,
                    "#browser": browser_info.type,
                    "#browser_version": browser_info.version,
                };
            },
            _getBrowser:function() {
                var browser = {'type':'','version':''};
                try{
                    var ua = window.navigator.userAgent.toLowerCase();
                    var versionMatch = [];
                    if(ua.match(/baidubrowser/) != null) {
                        browser['type'] = "baidu";
                        versionMatch.push(/baidubrowser\/([\d.]+)/);
                    }else if(ua.match(/bidubrowser/) != null) {
                        browser['type'] = "baidu";
                        versionMatch.push(/bidubrowser\/([\d.]+)/);
                    }else if(ua.match(/edga/) != null) {
                        browser['type'] = "edge";
                        versionMatch.push(/edga\/([\d.]+)/);
                    }else if(ua.match(/edgios/) != null) {
                        browser['type'] = "edge";
                        versionMatch.push(/edgios\/([\d.]+)/);
                    }else if(ua.match(/liebaofast/) != null) {
                        browser['type'] = "liebao";
                        versionMatch.push(/liebaofast\/([\d.]+)/);
                    }else if(ua.match(/sogoumobilebrowser/) != null) {
                        browser['type'] = "sogou";
                        versionMatch.push(/sogoumobilebrowser\/([\d.]+)/);
                    }else if(ua.match(/lbbrowser/) != null) {
                        browser['type'] = "liebao";
                        versionMatch.push(/lbbrowser\/([\d.]+)/);
                    }else if(ua.match(/crios/) != null) {
                        browser['type'] = "chrome";
                        versionMatch.push(/crios\/([\d.]+)/);
                    }else if(ua.match(/qihoobrowser/) != null) {
                        browser['type'] = "360";
                        versionMatch.push(/qihoobrowser\/([\d.]+)/);
                    }else if(ua.match(/mxios/) != null) {
                        browser['type'] = "maxthon";
                        versionMatch.push(/mxios\/([\d.]+)/);
                    }else if(ua.match(/fxios/) != null) {
                        browser['type'] = "firefox";
                        versionMatch.push(/fxios\/([\d.\w]+)/);
                    }else if(ua.match(/edge/) != null) {
                        browser['type'] = "edge";
                        versionMatch.push(/edge\/([\d.]+)/);
                    }else if(ua.match(/metasr/) != null) {
                        browser['type'] = "sogou";
                        versionMatch.push(/metasr ([\d.]+)/);
                    }else if(ua.match(/micromessenger/) != null) {
                        browser['type'] = "micromessenger";
                        versionMatch.push(/micromessenger\/([\d.]+)/);
                    }else if(ua.match(/mqqbrowser/) != null) {
                        browser['type'] = "qq";
                        versionMatch.push(/mqqbrowser\/([\d.]+)/);
                    }else if(ua.match(/qqbrowserlite/) != null){
                        browser['type'] = "qq";
                        versionMatch.push(/qqbrowserlite\/([\d.]+)/);
                    }else if(ua.match(/tencenttraveler/) != null){
                        browser['type'] = "qq";
                        versionMatch.push(/tencenttraveler\/([\d.]+)/);
                    }else if(ua.match(/qqbrowser/) != null) {
                        browser['type'] = "qq";
                        versionMatch.push(/qqbrowser\/([\d.]+)/);
                    }else if (ua.match(/maxthon/) != null) {
                        browser['type'] = "maxthon";
                        versionMatch.push(/maxthon\/([\d.]+)/);
                    }else if(ua.match(/ubrowser/) != null) {
                        browser['type'] = "uc";
                        versionMatch.push(/ubrowser\/([\d.]+)/);
                    }else if(ua.match(/ucbrowser/) != null) {
                        browser['type'] = "uc";
                        versionMatch.push(/ucbrowser\/([\d.]+)/);
                    }else if(ua.match(/firefox/) != null) {
                        browser['type'] = "firefox";
                        versionMatch.push(/firefox\/([\d.]+)/);
                    }else if(ua.match(/opera/) != null) {
                        browser['type'] = "opera";
                        versionMatch.push(/opera\/([\d.]+)/);
                    }else if(ua.match(/opr/) != null) {
                        browser['type'] = "opera";
                        versionMatch.push(/opr\/([\d.]+)/);
                    }else if(ua.match(/chrome/) != null) {
                        browser['type'] = 'chrome';
                        versionMatch.push(/chrome\/([\d.]+)/);
                    }else if(ua.match(/safari/) != null) {
                        browser['type'] = "safari";
                        versionMatch.push(/safari\/([\d.]+)/);
                    }else if(ua.match(/trident/) != null || ua.match(/msie/) != null) {
                        browser['type'] = "ie";
                    }

                    if(browser['type'] == "ie")
                    {
                        var tridentVersion = ua.match(/trident\/([\d.]+)/) ? ua.match(/trident\/([\d.]+)/)[1] : '';
                        var msieVersion = ua.match(/msie ([\d.]+)/) ? ua.match(/msie ([\d.]+)/)[1] : '';
                        
                        if(tridentVersion !== '')
                        {
                            browser['version'] = String(parseInt(tridentVersion) + 4);
                        }
                        else if(msieVersion !== '')
                        {
                            browser['version'] = msieVersion;
                        }
                    }
                    else if(versionMatch){
                        browser['version'] = ua.match(versionMatch[0]) ? ua.match(versionMatch[0])[1] : '';
                    }
                }catch(e){
                }
                return browser;
            },
            currentProps: {},
        };

        var tdSend = {
            _sendState:{
                _complete:0,
                _receive:0
            },
            _getSendCall:function(data, callback) {
                data._nocache = (String(Math.random()) + String(Math.random()) + String(Math.random())).replace(/\./g,'').slice(0,15);
                var originData = data;
                tdLog._info(originData);

                data['#app_id'] = td.param.appId;
                data = JSON.stringify(data);

                if(td.param.useAppTrack){
                    if((typeof ThinkingData_APP_JS_Bridge === 'object') && ThinkingData_APP_JS_Bridge.thinkingdata_track){
                        ThinkingData_APP_JS_Bridge.thinkingdata_track(data);
                        (typeof callback === 'function') && callback();
                    }else if(/td-sdk-ios/.test(navigator.userAgent) && !window.MSStream){
                        var iframe = document.createElement('iframe');
                        iframe.setAttribute('src', 'thinkinganalytics://trackEvent?event=' + tdCommon._code._encodeURIComponent(data));
                        document.documentElement.appendChild(iframe);
                        iframe.parentNode.removeChild(iframe);
                        iframe = null;
                        (typeof callback === 'function') && callback();
                    }else{
                        tdSend._prepareServerUrl(data,callback);
                    }
                }else{
                    tdSend._prepareServerUrl(data,callback);
                }
            },
            _getUrlPara:function(url,data){
                var base64Data = tdCommon._code._base64Encode(data);
                var crc = 'crc=' + tdCommon._code._hashCode(base64Data);
                url = (url.indexOf('?') !== -1) ? url : url+'?';
                return url+'&data=' + tdCommon._code._encodeURIComponent(base64Data) + '&ext=' + tdCommon._code._encodeURIComponent(crc);
            },
            _prepareServerUrl:function(data,callback){
                if(tdCommon._check._isArray(td.param.serverUrl)){
                    for(var i =0; i<td.param.serverUrl.length;i++){
                        tdSend._sendCall(tdSend._getUrlPara(td.param.serverUrl[i],data),callback);
                    }
                }else{
                    tdSend._sendCall(tdSend._getUrlPara(td.param.serverUrl,data),callback);
                }
            },
            _stateInfo:function(param){

                this.callback = param.callback;
                this.hasCalled = false;
                this.serverUrl = param.serverUrl;
                this.sendState = param.sendState;

                if(td.param.send_method === 'image')
                {
                    this.img = document.createElement('img');

                    function callAndDelete(){
                        if(typeof this === 'object' && typeof this.callback === 'function' && !this.hasCalled){
                            this.hasCalled = true;
                            this.callback();
                        }
                    }
                    setTimeout(callAndDelete, td.param.callbackTimeout);

                    this.img.onload = function(e) {
                        this.onload = null;
                        ++tdSend._sendState._complete;
                        callAndDelete();
                    };
                    this.img.onerror = function(e) {
                        this.onerror = null;
                        callAndDelete();
                    };
                    this.img.onabort = function(e) {
                        this.onabort = null;
                        callAndDelete();
                    };
                    this.img.src = this.serverUrl;
                }
                else if(td.param.send_method === 'ajax')
                {
                    var urlData = '';
                    var url = '';
                    if(this.serverUrl.indexOf('?') !== -1){
                        urlData = this.serverUrl.split('?');
                        if(urlData.length == 2){
                            url = urlData[0];
                            urlData = urlData[1];
                        }
                    }
                    if(urlData != ''){
                        var xhr = null;
                        if(window.XMLHttpRequest){
                            xhr = new XMLHttpRequest();
                        } else {
                            xhr = new ActiveXObject('Microsoft.XMLHTTP');
                        }
                        xhr.open('post',url, true);
                        xhr.setRequestHeader("Content-type","application/x-www-form-urlencoded");
                        xhr.send(urlData);
                        xhr.onreadystatechange = function () {
                            if (xhr.readyState == 4 && xhr.status == 200) {}
                        };
                        setTimeout(function () {
                            xhr.abort();
                        }, td.param.callbackTimeout);
                    }
                }
            },
            _sendCall:function(serverUrl,callback){
                ++tdSend._sendState._receive;
                var state = '_state' + this._receive;
                this[state] = new this._stateInfo({
                    callback: callback,
                    serverUrl: serverUrl,
                    sendState: this
                });
            }
        };

        function appJsBridge(){
            var app_info = null;
            var todo = null;
            function setAppInfo(data){
                app_info = data;
                if(tdCommon._check._isJSONString(app_info)){
                    app_info = JSON.parse(app_info);
                }
                if(todo){
                    todo(app_info);
                }
            }

            function getAndroid(){
                if(typeof window.ThinkingData_APP_JS_Bridge === 'object' && window.ThinkingData_APP_JS_Bridge.thinkingdata_call_app){
                    app_info = ThinkingData_APP_JS_Bridge.thinkingdata_call_app();
                    if(tdCommon._check._isJSONString(app_info)){
                        app_info = JSON.parse(app_info);
                    }
                }
            }

            window.thinkingdata_app_js_bridge_call_js = function(data){
                setAppInfo(data);
            };

            function calliOS() {
                if (/iPad|iPhone|iPod/.test(navigator.userAgent) && !window.MSStream) {
                    var iframe = document.createElement("iframe");
                    iframe.setAttribute("src", "thinkinganalytics://getAppInfo");
                    document.documentElement.appendChild(iframe);
                    iframe.parentNode.removeChild(iframe);
                    iframe = null;
                }
            }
            td.getAppStatus = function(func){
                calliOS();

                getAndroid();

                if(!func){
                    return app_info;
                }else{
                    if(app_info === null){
                        todo = func;
                    }else{
                        func(app_info);
                    }
                }
            };
        };

        var tdFactory = {
            autoTrack: function(param, callback) {
                param = tdCommon._check._isObject(param) ? param : {};
                param = tdCommon._extend({
                    '#referrer': tdCommon._getReferrer(),
                    '#referrer_host': td.pageProp.referrer_host,
                    '#url': location.href,
                    '#url_path': location.pathname,
                    '#title': document.title
                },param);
                td.track('ta_pageview',param,callback
                );
            }
        };
        
        td.trackLink = function(dom,event_name,event_prop){
            if(dom && tdCommon._check._isObject(dom)){
                var HasPropName = true;
                if(!event_prop['name']){
                    HasPropName = false;
                }
                for(var type in dom){
                    var link = dom[type];
                    if(link && tdCommon._check._isArray(link)){
                        var length = 0;
                        switch (type){
                            case 'tag':
                                for(var i=0;i<link.length;i++){
                                    length = 0;
                                    length = document.getElementsByTagName(link[i]).length;
                                    for(var d=0;d<length;d++){
                                        document.getElementsByTagName(link[i])[d].addEventListener('click', function() {
                                            if(HasPropName)
                                            {
                                                td.track(event_name,event_prop);
                                            }else{
                                                event_prop['name'] = this.getAttribute("td-name") || this.innerHTML || this.value || '未获取标识';
                                                td.track(event_name,event_prop);
                                            }
                                        });
                                    }
                                }
                                break;
                            case 'class':
                                for(var i=0;i<link.length;i++){
                                    length = 0;
                                    length = document.getElementsByClassName(link[i]).length;
                                    for(var d=0;d<length;d++){
                                        document.getElementsByClassName(link[i])[d].addEventListener('click', function() {
                                            if(HasPropName)
                                            {
                                                td.track(event_name,event_prop);
                                            }else{
                                                event_prop['name'] = this.getAttribute("td-name") || this.innerHTML || this.value || '未获取标识';
                                                td.track(event_name,event_prop);
                                            }
                                        });
                                    }
                                }
                                break;
                            case 'id':
                                for(var i=0;i<link.length;i++) {
                                    let element = document.getElementById(link[i]);
                                    if (element != null) {
                                        document.getElementById(link[i]).addEventListener('click', function () {
                                            if (!event_prop['name'])
                                                if (HasPropName) {
                                                    td.track(event_name, event_prop);
                                                } else {
                                                    event_prop['name'] = this.getAttribute("td-name") || this.innerHTML || this.value || '未获取标识';
                                                    td.track(event_name, event_prop);
                                                }
                                        });
                                    }
                                }
                                break;
                            }
                    }
                }   
            }
        };

        td.setPageProperty = function(obj) {
            if (tdEvent._check({properties: obj})) {
                tdCommon._extend(tdConstruct.currentProps, obj);
            } else {
                logger.info('PageProperty输入的参数有误');
            }
        };

        td.login = function(id){
            if(typeof id === 'number'){
                id = String(id);
            }
            if (tdEvent._check({account_id: id})) {
                var accountId = tdStore._getAccountId();
                if(id !== accountId){
                    tdUser._trackSignup(id,'signup');
                }
            } else {
                tdLog._info('login的参数必须是字符串');
            }
        };
        td.logout = function(isChangeId){
            if(isChangeId === true){
                var distinct_id = tdUUID();
                tdStore._set('distinct_id',distinct_id);
                tdStore._state.distinct_id = distinct_id;
            }
            tdStore._set('account_id','');
            tdStore._state.account_id = '';
        };

        td.userSet = function(p, c) {
            if (tdEvent._check({propertiesMust: p})) {
                tdEvent._send({
                    type: 'user_set',
                    properties: p
                }, c);
            }
        };

        td.userSetOnce = function(p, c) {
            if (tdEvent._check({propertiesMust: p})) {
                tdEvent._send({
                    type: 'user_setOnce',
                    properties: p
                }, c);
            }
        };

        td.userAdd = function(p, c) {
            var str = p;
            if (tdCommon._check._isString(p)) {
                p = {}
                p[str] = 1;
            }
            function isChecked(p) {
                for (var i in p) {
                    if (!/-*\d+/.test(String(p[i]))) {
                        return false;
                    }
                }
                return true;
            }

            if (tdEvent._check({propertiesMust: p})) {
                if (isChecked(p)) {
                    tdEvent._send({
                        type: 'user_add',
                        properties: p
                    }, c);
                } else {
                    logger.info('profile_increment的值只能是数字');
                }
            }
        };

        td.userDel = function(c) {
            tdEvent._send({
                type: 'user_del'
            }, c);
        };

        td.track = function(e, p, c) {
            if (tdEvent._check({event: e, properties: p})) {
                tdEvent._send({
                    type: 'track',
                    event: e,
                    properties: p
                }, c);
            }
        };

       td.identify = function (id) {
           if(typeof id === 'number'){
               id = String(id);
           }
           if (tdEvent._check({distinct_id: id})) {
               var distinctId = tdStore._getDistinctId();
               if(id !== distinctId){
                   tdStore._set('distinct_id', id);
                   tdStore._state.distinct_id = id;
               }
           } else {
               tdLog._info('identify 的参数必须是字符串');
           }
       };

    td.quick = function() {
            var arg = slice.call(arguments);
            var arg0 = arg[0];
            var arg1 = arg.slice(1);
            if (typeof arg0 === 'string' && tdFactory[arg0]) {
                return tdFactory[arg0].apply(tdFactory, arg1);
            } else if (typeof arg0 === 'function') {
                arg0.apply(td, arg1);
            } else {
                tdLog._info('quick方法中没有这个功能' + arg[0]);
            }
        };
        td.init = function(param){
            if((!param && isDeclare) || (param && !isDeclare)){
                tdConstruct._paramInit(param);
                appJsBridge();
                tdConstruct._initPage();
                tdStore._init();
                if(td._q && tdCommon._check._isArray(td._q) && td._q.length > 0 ){
                    tdEach(td._q, function(content) {
                        td[content[0]].apply(td, slice.call(content[1]));
                    });
                }
            }
        };
        td.init();
        return td;
    }catch(err){
        if (typeof console === 'object' && console.log) {
            try {
                console.log(err)
            } catch (e) {

            }
        }
    }
})
